package com.example.controlecarrro2;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.gesture.Gesture;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
//import com.chiralcode.colorpicker.ColorPicker;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity implements  View.OnTouchListener{
    public ImageView v1;

    private Socket Socket;

    private static final int SERVERPORT = 1337;
    private static final String SERVER_IP = "192.168.0.138";

    //private ColorPicker colorPicker;
    private Button button;
    private EditText texto;
    private boolean newColor = false;
    private int r, g, b;
    private String RGBstr;
    boolean whiteLedStat = false; //Segna lo stato del bottone per distinguere acceso o spento
    boolean fadeStat = false;
    public List<ScanResult> mScanResults;
    WifiManager mWifiManager;
    public TextView tv;
    public Handler mainHandler = new Handler(Looper.getMainLooper());
    public BluetoothDevice device=null;
    public BluetoothSocket mBTSocket = null;
    public BluetoothAdapter mBTAdapter;
    public Thread tarefa_conectar_hc;
    public int i=0;
    public Runnable executavel;
    public long tempo=100;
    private ImageButton frente_bot;
    private ImageButton tras_bot;
    private ImageButton dir_bot;
    public Rect rect;
    private ImageButton esq_bot;
    private Runnable movimentos_list;
    public List<Integer>lista_id_motor_ctr;
   // private String comando;
    private ToggleButton farol_baixo_bot;
    private Drawable bg_buzina;
    private Drawable bg_farol_bx;
    private ImageButton buzina_bot;
    private ImageButton pisca_bot;
    private ImageButton som_bot;
    private TextView velocidade;
    private SeekBar ajuste_velocidade;
    float maxima_velocidade = 130;
    int valor_vel=0;
    /*private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWifiManager.getScanResults();
                // add your logic here
            }
        }
    };

    private void getWifiNetworksList() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        final WifiManager wifiManager =
                (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ;
        registerReceiver(new BroadcastReceiver() {


            @Override
            public void onReceive(Context context, Intent intent) {
                StringBuilder sb = new StringBuilder();

                List<ScanResult> scanList = wifiManager.getScanResults();
                sb.append("\n  Number Of Wifi connections :" + " " + scanList.size() + "\n\n");
                for (int i = 0; i < scanList.size(); i++) {
                    sb.append(new Integer(i + 1).toString() + ". ");
                    sb.append((scanList.get(i)).toString());
                    sb.append("\n\n");
                }

                tv.setText(sb);
            }

        }, filter);
        wifiManager.startScan();
    }

    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // "random" unique identifier
            // );
        } catch (Exception e) {
            Log.e(MainActivity.class.getSimpleName(), "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }*/

    public boolean conecta_hc(){

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);

        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);


        }
        try {
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
            String mac = "98:D3:31:F9:4F:66";
            device = mBTAdapter.getRemoteDevice(mac);//"8C:90:2D:42:1B:00");
        } catch (Exception e){
            Toast.makeText(getBaseContext(), "Falha no endereço!", Toast.LENGTH_SHORT).show();
            return false;
        }


        try {

            mBTSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {

            Toast.makeText(getBaseContext(), "Falha na UUID", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Establish the Bluetooth socket connection.

        try {

            mBTAdapter.cancelDiscovery();
            mBTSocket.connect();
            Toast.makeText(getBaseContext(), "Conectado com sucesso!", Toast.LENGTH_SHORT).show();

            return true;
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Falha ao conectar. O carro está perto?", Toast.LENGTH_SHORT).show();

            try {
                mBTSocket.close();

            } catch (IOException e2) {
                //insert code to deal with this
             }
            return false;
        }



    }


    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //getWifiNetworksList() ;
        //ListView lista = (ListView)findViewById(R.id.wifi);

        // 4. Set the adapter on the ListView
        //lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mWifiManager.getScanResults()));




       /*tarefa_conectar_hc= new Thread() {
            @Override
            public void run() {
                conecta_hc();
        }}.start();*/
        //final boolean conectou=conecta_hc();




        //new Thread(new ClientThread()).start();

        //colorPicker = (ColorPicker) findViewById(R.id.colorPicker);
        //final Animation clickAnim = AnimationUtils.loadAnimation(this, R.anim.frente_anim);

        frente_bot = (ImageButton) findViewById(R.id.frente);
        tras_bot = (ImageButton) findViewById(R.id.tras);
        dir_bot=(ImageButton) findViewById(R.id.direita);
        esq_bot=(ImageButton) findViewById(R.id.esquerda);
        //farol_baixo_bot=(ToggleButton)findViewById(R.id.farol_baixo);
       // buzina_bot=(ImageButton)findViewById(R.id.buzina);
        //pisca_bot=(ImageButton)findViewById(R.id.pisca_farol);
        //som_bot=(ImageButton)findViewById(R.id.som_aceleracao);
        frente_bot.setOnTouchListener(this);
        tras_bot.setOnTouchListener(this);
        esq_bot.setOnTouchListener(this);
        dir_bot.setOnTouchListener(this);
        velocidade = findViewById(R.id.txtValorFader);
        ajuste_velocidade = findViewById(R.id.seekBar);
        ajuste_velocidade.setClickable(false);
        ajuste_velocidade.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Pegamos o retângulo que envolve o "botão" (Thumb)
                    Rect limitesThumb = ajuste_velocidade.getThumb().getBounds();

                    // ATENÇÃO: Como o fader está em 270°, o toque vertical (Y)
                    // do usuário agora corresponde ao eixo horizontal (X) interno do SeekBar.
                    // Precisamos converter a coordenada do toque para o sistema do componente.

                    float toqueConvertido = event.getY();
                    // Invertemos a lógica: o topo da tela é o fim do progresso
                    int posicaoRealNoEixo = (int) (ajuste_velocidade.getWidth() - toqueConvertido);

                    // Verifica se o toque do usuário está FORA da área do botão
                    if (posicaoRealNoEixo < limitesThumb.left || posicaoRealNoEixo > limitesThumb.right) {
                        return true; // Bloqueia o pulo
                    }
                }
                return false; // Permite arrastar se clicou no botão
            }
        });
        ajuste_velocidade.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                // Atualiza o texto com o valor atual do progresso
                frente_bot.setEnabled(false);
                tras_bot.setEnabled(false);
                esq_bot.setEnabled(false);
                dir_bot.setEnabled(false);
                valor_vel=(int) ((maxima_velocidade*progress)/100.0+120);
                mandar_comando(String.valueOf(valor_vel)+";");

                String ffdf=String.valueOf(valor_vel);
                velocidade.setText(String.valueOf(progress));
                // 2. CALCULAR A COR DINÂMICA (Azul -> Vermelho)
                int red = (int) (progress * 2.55);
                int blue = (int) (255 - (progress * 2.55));
                int mainColor = android.graphics.Color.rgb(red, 0, blue);
                velocidade.setTextColor(mainColor);

                // 3. EFEITO SHAKE (Da resposta anterior)
                float shakeFactor = 5f;
                velocidade.setTranslationX((float) (Math.random() * shakeFactor - (shakeFactor / 2*Math.random() )));
                velocidade.setTranslationY((float) (Math.random() * shakeFactor - (shakeFactor / 2*Math.random() )));

                // 4. EFEITO GLOW DINÂMICO (NOVO!)
                // Definimos o raio do brilho. Aumente para um brilho maior (ex: 30f).
                float glowRadius = 20f;

                // Criamos uma cor para o brilho. O segredo é ter ALTA saturação.
                // Usamos a mesma cor principal, mas você poderia usar um tom mais claro.
                int glowColor = android.graphics.Color.rgb(red, 0, blue);

                // Aplicamos a camada de sombra (raio, dx, dy, cor)
                // dx e dy como 0f garantem que o brilho seja uniforme ao redor do texto.
                velocidade.setShadowLayer(glowRadius, 0f, 0f, glowColor);


                // Dica: Se quiser simular dB (ex: de -100 a 0)
                // int dbValue = progress - 100;
                // txtValor.setText(dbValue + " dB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //mandar_comando("P");

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                frente_bot.setEnabled(true);
                tras_bot.setEnabled(true);
                esq_bot.setEnabled(true);
                dir_bot.setEnabled(true);
                // Opcional: voltar a cor original ao soltar

            }
        });
        //buzina_bot.setOnTouchListener(this);
        //pisca_bot.setOnTouchListener(this);
        //som_bot.setOnTouchListener(this);
        /*farol_baixo_bot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean isChecked) {
                if (isChecked) {
                    bg_farol_bx=cb.getBackground();
                    cb.getCompoundDrawables()[1].setColorFilter(0xFFFFD700, PorterDuff.Mode.SRC_ATOP);
                    anima_alpha(cb);
                } else {
                    cb.getCompoundDrawables()[1].clearColorFilter();
                    anima_alpha(cb);
                    // Code to execute when the toggle button is OFF
                }
            }
        });*/

      
    }

    public void anima_alpha(View v){
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "Alpha", 0.5f, 1);
        anim.setDuration(1000);
        anim.start();
    }
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int id = view.getId();
        String comando = "";
        if (id == R.id.direita || id == R.id.esquerda || id == R.id.frente || id == R.id.tras) {

            if (event.getAction() == MotionEvent.ACTION_DOWN ||event.getAction() == MotionEvent.ACTION_MOVE  || event.getAction() == MotionEvent.ACTION_HOVER_ENTER){
                ajuste_velocidade.setEnabled(false);
                if (id == R.id.direita) {
                    comando = "R";
                    esq_bot.setEnabled(false);
                }
            if (id == R.id.esquerda) {
                comando = "L";
                dir_bot.setEnabled(false);
            }
            if (id == R.id.frente) {
                comando = "F";
                tras_bot.setEnabled(false);
            }
            if (id == R.id.tras) {
                comando = "T";
                frente_bot.setEnabled(false);
            }
            mandar_comando(comando);
            v1 = (ImageView) view;
            v1.getDrawable().setColorFilter(0x90990000, PorterDuff.Mode.SRC_ATOP);
            anima_alpha(view);

            }
            Log.d("MyTag", "TESTE: "+ event.getAction());



        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            ajuste_velocidade.setEnabled(true);
            if (id == R.id.direita) {
                comando = "Z";
                esq_bot.setEnabled(true);
            }
            if (id == R.id.esquerda) {
                comando = "X";
                dir_bot.setEnabled(true);
            }
            if (id == R.id.frente) {
                comando = "Y";
                tras_bot.setEnabled(true);
            }
            if (id == R.id.tras) {
                comando = "W";
                frente_bot.setEnabled(true);
            }
            v1 = (ImageView) view;
            v1.getDrawable().clearColorFilter();
            mandar_comando(comando);
            //esq_bot.setEnabled(true);
        }

        }

//        if(id==R.id.buzina){
//
//
//                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_HOVER_ENTER)
//                {
//                    //Add delay(200) on arduino side
//
//                    anima_vibra((ImageButton)view,"X");
//                    mandar_comando("B");
//
//                }
//
//            }
//
//        if(id==R.id.pisca_farol) {
//
//            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
//                ((ImageButton) view).getDrawable().setColorFilter(0xFFFFD700, PorterDuff.Mode.SRC_ATOP);
//                anima_alpha(view);
//                //mandar_comando("P"); //inicia o farol alto
//            }
//            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
//                ((ImageButton)view).getDrawable().clearColorFilter();
//                anima_alpha(view);
//                //mandar_comando("F"); //fecha farol
//            }
//        }
//        if (id==R.id.som_aceleracao){
//                ImageButton ib=(ImageButton) view;
//                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
//                    ib.getDrawable().setColorFilter(0xFFFF0000, PorterDuff.Mode.SRC_ATOP);
//                    anima_vibra(ib, "Y");
//                    //mandar_comando("S");
//                }
//            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
//                ib.getDrawable().clearColorFilter();
//                //mandar_comando("F"); //fecha farol
//            }
//
//
//        }


        return false;
    }
    public void anima_vibra(View ib,String o){
        ObjectAnimator vibracao = ObjectAnimator.ofFloat(
                ib,
                "rotation"+o,(float)(10*Math.random()+10) , // Start color (e.g., black)
                -(float)(10*Math.random()+10) // End color (e.g., gold/shining)
        );
        vibracao.setDuration(Math.round(50*Math.random()+50)); // Duration in milliseconds
        vibracao.setRepeatCount(6); // Loop the animation
        vibracao.setRepeatMode(ObjectAnimator.REVERSE);
        vibracao.reverse();
        vibracao.addListener(new AnimatorListenerAdapter() {
            private boolean reversed = false;
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ib.setRotationX(0);
                ib.setRotationY(0);
            }
        });
        // Reverse the animation when it reaches the end
        vibracao.start();

    }
    public void mandar_comando(String comando){

        try {
            OutputStream outStream = mBTSocket.getOutputStream();

            outStream.write(comando.getBytes());

        }catch(Exception e){
            i +=1;
            Toast.makeText(getBaseContext(), "A conexão foi perdida... tentativa: " + i, Toast.LENGTH_SHORT).show();

            conecta_hc();
            //stopRepeating();
        }


    }


    /*public void mover_direita_esquerda(View view,MotionEvent event,ImageButton bot1,ImageButton bot2){

        //Log.e("Teste4",""+event.getX());
        ImageView v1= (ImageView) view;
        v1.getDrawable().clearColorFilter();

        if (isPointInsideView(bot2, event.getRawX(), event.getRawY())){

            bot1.setEnabled(false);

            bot2.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));

        }



    }*/


    public boolean isPointInsideView(View view, float x, float y) {
        Rect outRect = new Rect();
        view.getDrawingRect(outRect);
        // Convert view coordinates to screen coordinates if necessary
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains((int)x, (int)y);
    }



    private void startRepeating(String action) {
        // Clear existing if any
        if (executavel!=null) {
            //stopRepeating();
        }
        executavel=new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outStream = mBTSocket.getOutputStream();

                    outStream.write(action.getBytes());

                }catch(IOException e){
                    i +=1;
                    Toast.makeText(getBaseContext(), "A conexão foi perdida... tentativa: " + i, Toast.LENGTH_SHORT).show();

                    conecta_hc();
                    //stopRepeating();
                }
                // This code runs on the main (UI) thread
                Toast.makeText(getBaseContext(), "teste", Toast.LENGTH_SHORT).show();
                mainHandler.postDelayed(this,tempo);
            }
        };

        mainHandler.post(executavel);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Handle global touch events here
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:

                if (isPointInsideView(dir_bot, event.getX(), event.getY())) {



                }


                //Log.d("GlobalTouch", "X: " + event.getX() + ", Y: " + event.getY());
                break;
            // ... handle other actions
        }
        return super.onTouchEvent(event);
    }



/*
    private void gesture() {
        // Clear existing if any
        TableLayout touchview =
        movimentos_list=new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < touchview.getChildCount(); i++) {
                    View current = touchview.getChildAt(i);
                    if (current instanceof Button) {
                        Button b = (Button) current;

                        if (!isPointWithin(x, y, b.getLeft(), b.getRight(), b.getTop(),
                                b.getBottom())) {
                            b.getBackground().setState(defaultStates);
                        }

                        if (isPointWithin(x, y, b.getLeft(), b.getRight(), b.getTop(),
                                b.getBottom())) {
                            b.getBackground().setState(STATE_PRESSED);
                            if (b != mLastButton) {
                                mLastButton = b;
                                buttonIndicator.setText(mLastButton.getText());
                            }
                        }

                    }


                mainHandler.post(this);
            }


        mainHandler.post(movimentos_list);
    }

    private void stopRepeating() {
        mainHandler.removeCallbacks(executavel);
    }


/*
    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                Socket = new Socket(serverAddr, SERVERPORT);

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }*/

}