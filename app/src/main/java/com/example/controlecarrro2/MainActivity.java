package com.example.controlecarrro2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final String DEFAULT_MAC = "98:D3:31:F9:4F:66";
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH = 100;
    private static final int MIN_PWM = 120;
    private static final int MAX_PWM = 250;

    // Uma fila de E/S preserva a ordem: movimento -> soltar -> parar.
    private final ExecutorService bluetoothIo = Executors.newSingleThreadExecutor();
    private volatile BluetoothSocket socket;
    private volatile boolean connected;
    private boolean connecting;
    private boolean armed;
    private int activeDrive = View.NO_ID;
    private int activeSteering = View.NO_ID;
    private int pwm = MIN_PWM;
    private String bluetoothMac;

    private TextView statusText;
    private TextView speedText;
    private Button connectButton;
    private Button configureButton;
    private Button armButton;
    private Button stopButton;
    private SeekBar speedSlider;
    private ImageButton forwardButton, reverseButton, leftButton, rightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        bluetoothMac = getPreferences(MODE_PRIVATE).getString("bluetooth_mac", DEFAULT_MAC);
        statusText = findViewById(R.id.connectionStatus);
        speedText = findViewById(R.id.txtValorFader);
        connectButton = findViewById(R.id.connectButton);
        configureButton = findViewById(R.id.configureButton);
        armButton = findViewById(R.id.armButton);
        stopButton = findViewById(R.id.stopButton);
        speedSlider = findViewById(R.id.seekBar);
        forwardButton = findViewById(R.id.frente);
        reverseButton = findViewById(R.id.tras);
        leftButton = findViewById(R.id.esquerda);
        rightButton = findViewById(R.id.direita);

        for (ImageButton button : new ImageButton[] {
                forwardButton, reverseButton, leftButton, rightButton}) {
            button.setOnTouchListener(this);
        }

        connectButton.setOnClickListener(v -> {
            if (connected) disconnectBluetooth();
            else connectBluetooth();
        });
        configureButton.setOnClickListener(v -> configureBluetoothMac());
        armButton.setOnClickListener(v -> {
            if (armed) emergencyStop();
            else {
                armed = true;
                updateInterface();
            }
        });
        stopButton.setOnClickListener(v -> emergencyStop());

        speedSlider.setProgress(0);
        updateSpeed(0, false);
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                updateSpeed(progress, fromUser);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        // Evita saltos bruscos: a regulagem começa somente ao tocar no cursor.
        speedSlider.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                float tolerance = 30 * getResources().getDisplayMetrics().density;
                if (speedSlider.getThumb() != null
                        && Math.abs(event.getX() - speedSlider.getThumb().getBounds().centerX())
                        > tolerance) return true;
            }
            return false;
        });
        updateInterface();
    }

    private void updateSpeed(int progress, boolean fromUser) {
        pwm = MIN_PWM + (MAX_PWM - MIN_PWM) * progress / 100;
        speedText.setText(getString(R.string.speed_label, progress));
        if (fromUser && connected && armed) sendCommand(pwm + ";");
    }

    private void configureBluetoothMac() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setSingleLine(true);
        input.setText(bluetoothMac);
        input.selectAll();
        new AlertDialog.Builder(this)
                .setTitle(R.string.configure_title)
                .setMessage(R.string.configure_hint)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String mac = input.getText().toString().trim()
                            .toUpperCase(Locale.ROOT);
                    if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
                        Toast.makeText(this, R.string.invalid_mac, Toast.LENGTH_LONG).show();
                        return;
                    }
                    bluetoothMac = mac;
                    getPreferences(MODE_PRIVATE).edit()
                            .putString("bluetooth_mac", bluetoothMac).apply();
                    updateInterface();
                }).show();
    }

    private void connectBluetooth() {
        if (connected || connecting) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, R.string.enable_bluetooth, Toast.LENGTH_LONG).show();
            return;
        }
        connecting = true;
        armed = false;
        updateInterface();
        bluetoothIo.execute(() -> {
            BluetoothSocket candidate = null;
            try {
                BluetoothDevice device = adapter.getRemoteDevice(bluetoothMac);
                candidate = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket = candidate;
                candidate.connect();
                // Sempre começa com motor e direção parados, sem liberar os botões.
                candidate.getOutputStream().write(
                        ("YZ" + pwm + ";").getBytes(StandardCharsets.US_ASCII));
                connected = true;
                runOnUiThread(() -> {
                    connecting = false;
                    armed = false;
                    updateInterface();
                });
            } catch (Exception failure) {
                closeSocket(candidate);
                if (socket == candidate) socket = null;
                connected = false;
                runOnUiThread(() -> {
                    connecting = false;
                    emergencyStop();
                    Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_LONG).show();
                    updateInterface();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBluetooth();
            } else {
                Toast.makeText(this, R.string.bluetooth_permission, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendCommand(String command) {
        if (!connected) return;
        bluetoothIo.execute(() -> {
            BluetoothSocket current = socket;
            if (current == null || !connected) return;
            try {
                current.getOutputStream().write(command.getBytes(StandardCharsets.US_ASCII));
            } catch (IOException failure) {
                closeSocket(current);
                if (socket == current) {
                    socket = null;
                    connected = false;
                    runOnUiThread(() -> {
                        armed = false;
                        clearPressedDirections();
                        updateInterface();
                        Toast.makeText(this, R.string.connection_lost, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void disconnectBluetooth() {
        emergencyStop();
        bluetoothIo.execute(() -> {
            BluetoothSocket current = socket;
            closeSocket(current);
            if (socket == current) socket = null;
            connected = false;
            runOnUiThread(() -> {
                connecting = false;
                updateInterface();
            });
        });
    }

    private static void closeSocket(BluetoothSocket value) {
        if (value == null) return;
        try { value.close(); } catch (IOException ignored) { }
    }

    private void clearPressedDirections() {
        activeDrive = View.NO_ID;
        activeSteering = View.NO_ID;
        for (ImageButton button : new ImageButton[] {
                forwardButton, reverseButton, leftButton, rightButton}) {
            if (button.getDrawable() != null) button.getDrawable().clearColorFilter();
        }
    }

    private void emergencyStop() {
        armed = false;
        clearPressedDirections();
        // O firmware atual usa Y/W para parar a tração e Z/X para parar a direção.
        sendCommand("YZ");
        updateInterface();
    }

    private void updateInterface() {
        if (connecting) statusText.setText(R.string.connecting);
        else if (!connected) statusText.setText(R.string.disconnected);
        else if (armed) statusText.setText(R.string.ready);
        else statusText.setText(R.string.blocked);
        statusText.setTextColor(getResources().getColor(
                connected && armed ? android.R.color.holo_green_light
                        : android.R.color.holo_orange_light));

        connectButton.setText(connected ? R.string.disconnect : R.string.connect);
        connectButton.setEnabled(!connecting);
        configureButton.setEnabled(!connecting && !connected);
        armButton.setText(armed ? R.string.lock_controls : R.string.unlock_controls);
        armButton.setEnabled(connected && !connecting);
        stopButton.setEnabled(true);
        boolean allowMovement = connected && armed;
        forwardButton.setEnabled(allowMovement);
        reverseButton.setEnabled(allowMovement);
        leftButton.setEnabled(allowMovement);
        rightButton.setEnabled(allowMovement);
        speedSlider.setEnabled(allowMovement
                && activeDrive == View.NO_ID && activeSteering == View.NO_ID);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (!connected || !armed) return true;
        int action = event.getActionMasked();
        int id = view.getId();
        boolean drive = id == R.id.frente || id == R.id.tras;
        if (action == MotionEvent.ACTION_DOWN) {
            if (drive && activeDrive != View.NO_ID) return true;
            if (!drive && activeSteering != View.NO_ID) return true;
            if (drive) activeDrive = id;
            else activeSteering = id;

            if (view instanceof ImageButton
                    && ((ImageButton) view).getDrawable() != null) {
                ((ImageButton) view).getDrawable().setColorFilter(
                        0xAAFF3333, PorterDuff.Mode.SRC_ATOP);
            }
            sendCommand(id == R.id.frente ? "F" : id == R.id.tras ? "T"
                    : id == R.id.esquerda ? "L" : "R");
            updateInterface();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getX() >= 0 && event.getX() <= view.getWidth()
                    && event.getY() >= 0 && event.getY() <= view.getHeight()) return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_MOVE) {
            if (drive && activeDrive == id) {
                activeDrive = View.NO_ID;
                sendCommand("Y");
            } else if (!drive && activeSteering == id) {
                activeSteering = View.NO_ID;
                sendCommand("Z");
            }
            if (view instanceof ImageButton
                    && ((ImageButton) view).getDrawable() != null) {
                ((ImageButton) view).getDrawable().clearColorFilter();
            }
            updateInterface();
            return true;
        }
        return true;
    }

    @Override
    protected void onPause() {
        emergencyStop();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && statusText != null) emergencyStop();
    }

    @Override
    protected void onDestroy() {
        emergencyStop();
        bluetoothIo.execute(() -> {
            closeSocket(socket);
            socket = null;
            connected = false;
        });
        bluetoothIo.shutdown();
        super.onDestroy();
    }
}
