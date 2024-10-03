package com.moutamid.givegetvalue;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.moutamid.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private static final String TAG = "MainActivity";
    int position__;
    public static String extractedValue = "";
    public static String extractedType = "";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String USER_ID_KEY = "UserID";
    public static EditText valueEditText, passwordEditText;
    public static Spinner typeSpinner;
    public static TextView balanceTextView;
    private Button readButton, requestButton, giveButton, addButton, quitCamera;
    private ImageView qrCodeImageView;
    public static float currentBalance = 0;
    private boolean isMasterUser = false;
    Button enterButton, userButton, masterButton, quitButton;
    String valueType;
    boolean is_giver = false;
    public static RelativeLayout confirmation_lyt;
    Button NoButton, yesButton;
    String status = "receiver";
    TextView tvLogs;
    boolean is_btn_click = false;


    ServerSocket server_serverSocket;
    Socket server_clientSocket;
    PrintStream server_ps = null;
    BufferedReader server_br = null;
    String server_serverName = "Server";
    String server_serverip = "";
    ImageView server_qrCodeImageView;
    String server_message;


    String client_ip, client_name;
    Socket client_clientSocket;
    PrintStream client_ps = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String userId = getUserId(this);
        Log.d("UserID", userId + "  ID");
        client_clientSocket = new Socket();
        server_serverip = Utils.getIPAddress(true);
        tvLogs = findViewById(R.id.tvLogs);
        yesButton = findViewById(R.id.yesButton);
        NoButton = findViewById(R.id.NoButton);
        confirmation_lyt = findViewById(R.id.confirmation_lyt);
        previewView = findViewById(R.id.camera_preview);
        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient();
        quitButton = findViewById(R.id.quitButton);
        requestButton = findViewById(R.id.requestButton);
        userButton = findViewById(R.id.userButton);
        enterButton = findViewById(R.id.enterButton);
        masterButton = findViewById(R.id.masterButton);
        passwordEditText = findViewById(R.id.passwordEditText);
        addButton = findViewById(R.id.addButton);
        quitCamera = findViewById(R.id.quitCamera);
        valueEditText = findViewById(R.id.valueEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        balanceTextView = findViewById(R.id.balanceTextView);
        readButton = findViewById(R.id.readButton);
        giveButton = findViewById(R.id.giveButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        float balance = Stash.getFloat(valueType + "_balance", 0);
        if (balance != 0) {
            currentBalance = balance;
        } else {
            currentBalance = 0;
        }

        if (currentBalance == 0) {
            giveButton.setVisibility(View.GONE);
        } else {
            giveButton.setVisibility(View.VISIBLE);
        }
        userButton.setBackgroundResource(R.drawable.btn_bg);
        masterButton.setBackgroundResource(R.drawable.btn_bg_lght);
        readButton.setVisibility(View.VISIBLE);
        requestButton.setVisibility(View.VISIBLE);
        Utils.checkApp(MainActivity.this);
        if (isMasterUser) {
            typeSpinner.setSelection(0);
        } else {
            typeSpinner.setSelection(Stash.getInt("position"));
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                Stash.put("position", position);
                position__ = position;
                if (position != 0) {
                    if (isMasterUser && position > 0) {
                        valueType = selectedItem;
                        float balance = Stash.getFloat(valueType + "_balance", 0);
                        float categoryBalance;
                        if (balance != 0) {
                            categoryBalance = balance;
                        } else {
                            categoryBalance = Float.valueOf(0); // Default value or handle appropriately
                        }
                        String formattedBalance = String.format("%.2f", categoryBalance);
                        balanceTextView.setText("Balance for " + valueType + ": " + formattedBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);
                        if (categoryBalance == 0) {
                            giveButton.setVisibility(View.GONE);
                            addButton.setVisibility(View.VISIBLE);
                        } else {
                            giveButton.setVisibility(View.VISIBLE);
                            addButton.setVisibility(View.VISIBLE);

                        }
                    } else {
                        String valueType = selectedItem;
                        float balance = Stash.getFloat(valueType + "_balance", 0);
                        float categoryBalance;
                        if (balance != 0) {
                            categoryBalance = balance;
                        } else {
                            categoryBalance = Float.valueOf(0); // Default value or handle appropriately
                        }

                        // Format categoryBalance to 2 decimal places
                        String formattedBalance = String.format("%.2f", categoryBalance);
                        balanceTextView.setText("Balance for " + valueType + ": " + formattedBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);

                        if (categoryBalance == 0) {
                            giveButton.setVisibility(View.GONE);
                        } else {
                            giveButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "master_passwords_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("password1", "abc");
            editor.putString("password2", "DqPn9tkdbPWN");
            editor.putString("password3", "auAVMN5Qf6PH");
            editor.apply();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("EncryptedSharedPrefs", "Exception while creating EncryptedSharedPreferences", e);
        }
        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmation_lyt.setVisibility(View.GONE);
            }
        });
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmation_lyt.setVisibility(View.GONE);
                byte[] bytes = "yes".getBytes(Charset.defaultCharset());
                server_message = "yes_confirm";
                Thread sendThread = new Thread(new server_SendThread());
                sendThread.start();
                Stash.put(valueType + "_balance", currentBalance);
                String formattedBalance = String.format("%.2f", currentBalance);
                balanceTextView.setText("Balance for " + valueType + ": " + formattedBalance);
                valueEditText.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.GONE);
                enterButton.setVisibility(View.GONE);
                userButton.setEnabled(false);
                masterButton.setEnabled(true);
                isMasterUser = false;
                readButton.setVisibility(View.VISIBLE);
                requestButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                typeSpinner.setVisibility(View.VISIBLE);
                balanceTextView.setVisibility(View.VISIBLE);
                quitButton.setVisibility(View.GONE);
                qrCodeImageView.setVisibility(View.GONE);
                valueEditText.setText("");
                giveButton.setVisibility(View.VISIBLE);
//                server_Close();
//                client_Close();
            }
        });
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                server_Close();
//                client_Close();
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                } else {
                    startCamera();
                    previewView.setVisibility(View.VISIBLE);
                    quitCamera.setVisibility(View.VISIBLE);
                }
            }
        });

        quitCamera.setOnClickListener(v -> {
            previewView.setVisibility(View.GONE);
            quitCamera.setVisibility(View.GONE);
            stopCamera();
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userButton.setBackgroundResource(R.drawable.btn_bg);
                masterButton.setBackgroundResource(R.drawable.btn_bg_lght);
                valueEditText.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.GONE);
                enterButton.setVisibility(View.GONE);
                userButton.setEnabled(false);
                masterButton.setEnabled(true);
                isMasterUser = false;
                readButton.setVisibility(View.VISIBLE);
                requestButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                typeSpinner.setVisibility(View.VISIBLE);
                balanceTextView.setVisibility(View.VISIBLE);
                giveButton.setVisibility(View.VISIBLE);
                quitButton.setVisibility(View.GONE);
                qrCodeImageView.setVisibility(View.GONE);
                valueEditText.setText("");
            }
        });
        userButton.setOnClickListener(v -> {
            userButton.setBackgroundResource(R.drawable.btn_bg);
            masterButton.setBackgroundResource(R.drawable.btn_bg_lght);
            valueEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.GONE);
            enterButton.setVisibility(View.GONE);
            userButton.setEnabled(false);
            masterButton.setEnabled(true);
            isMasterUser = false;
            readButton.setVisibility(View.VISIBLE);
            requestButton.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.GONE);
            typeSpinner.setVisibility(View.VISIBLE);
            balanceTextView.setVisibility(View.VISIBLE);
            quitButton.setVisibility(View.GONE);
            qrCodeImageView.setVisibility(View.GONE);
            valueEditText.setText("");
        });
        masterButton.setOnClickListener(v -> {
            masterButton.setBackgroundResource(R.drawable.btn_bg);
            userButton.setBackgroundResource(R.drawable.btn_bg_lght);
            passwordEditText.setVisibility(View.VISIBLE);
            valueEditText.setVisibility(View.GONE);
            enterButton.setVisibility(View.VISIBLE);
            masterButton.setEnabled(false);
            userButton.setEnabled(true);
            quitButton.setVisibility(View.GONE);
            qrCodeImageView.setVisibility(View.GONE);
            valueEditText.setText("");
            isMasterUser = true;
            readButton.setVisibility(View.GONE);
            typeSpinner.setEnabled(true);
            addButton.setVisibility(View.GONE);
            giveButton.setVisibility(View.GONE);
            requestButton.setVisibility(View.GONE);
            typeSpinner.setVisibility(View.GONE);
            balanceTextView.setVisibility(View.GONE);
            typeSpinner.setSelection(0);
            balanceTextView.setText("Balance: 0");

        });
        giveButton.setOnClickListener(v -> giveAmount());
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (valueType != null && !valueType.isEmpty()) {
                    String valueToAddStr = valueEditText.getText().toString();
                    if (!valueToAddStr.isEmpty()) {
                        float valueToAdd = Float.parseFloat(valueToAddStr);
                        addValueToBalance(valueType, valueToAdd);
                    } else {
                        Toast.makeText(MainActivity.this, "Please enter a value to add", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a type", Toast.LENGTH_SHORT).show();
                }
            }
        });
        requestButton.setOnClickListener(v -> requestAmount());

        enterButton.setOnClickListener(view -> addPasswordAsMaster());

    }

    private void startGiveProcess() {
        is_giver = true;
        is_btn_click = true;
        valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString();
        currentBalance = Stash.getFloat(valueType + "_balance", 0);
        if (valueToGive.isEmpty()) {
            Toast.makeText(this, "Please enter some value to give", Toast.LENGTH_SHORT).show();
            return;
        }
        if (valueType.equals("Select Type")) {
            Toast.makeText(this, "Please select any type", Toast.LENGTH_SHORT).show();
            return;
        } if (Float.parseFloat(valueToGive)>=currentBalance) {
            Toast.makeText(this, "Insufficient balance for transaction", Toast.LENGTH_SHORT).show();
            return;
        }
        giveButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        readButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.GONE);
        quitButton.setVisibility(View.VISIBLE);
        quitButton.setEnabled(false);
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        String qrData = "Type: " + valueType + ", Value: " + valueToGive + ", Timestamp: " + formattedDate + ", Status: " + "giver" + ", ServerName: " + server_serverName + ", ServerIP: " + server_serverip;
        generateQRCode(qrData);
        currentBalance -= Float.parseFloat(valueToGive);
        quitButton.setEnabled(true);
    }


    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(writer.encode(data, BarcodeFormat.QR_CODE, 512, 512));
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
            quitButton.setVisibility(View.VISIBLE);
            if(server_clientSocket!=null)
            {
                server_updateChatMessage("yes_confirm");
            }
            else
            {
            Thread serverThread = new Thread(new server_ServerThread());
            serverThread.start();
        } }catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bmp;
    }

    private boolean isMasterPassword(String inputPassword) {
        SharedPreferences sharedPreferences = null;
        String masterKeyAlias = null;

        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    "master_passwords_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("EncryptionError", "Error creating EncryptedSharedPreferences", e);
            return false;
        }
        return sharedPreferences.getAll().containsValue(inputPassword);
    }

    private void addPasswordAsMaster() {
        String enteredPassword = passwordEditText.getText().toString();
        if (!isMasterPassword(enteredPassword)) {
            Toast.makeText(this, "Login failed, Try Again", Toast.LENGTH_SHORT).show();
            passwordEditText.setText("");
            return;
        }
        Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show();
        passwordEditText.setText("");
        valueEditText.setVisibility(View.VISIBLE);
        passwordEditText.setVisibility(View.GONE);
        enterButton.setVisibility(View.GONE);
        readButton.setVisibility(View.VISIBLE);
        typeSpinner.setEnabled(true);
        addButton.setVisibility(View.VISIBLE);
        giveButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.VISIBLE);
        typeSpinner.setVisibility(View.VISIBLE);
        balanceTextView.setVisibility(View.VISIBLE);
        valueEditText.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        stopCamera();
    }

    private void addValueToBalance(String valueType, float valueToAdd) {
        float balance = Stash.getFloat(valueType + "_balance", 0);
        float categoryBalance;
        if (balance != 0) {
            categoryBalance = balance;
        } else {
            categoryBalance = Float.valueOf(0); // Default value or handle appropriately
        }
        Log.d("BalanceUpdate", "1   " + categoryBalance);
        categoryBalance += valueToAdd;
        Log.d("BalanceUpdate", "2   " + categoryBalance);
        Stash.put(valueType + "_balance", categoryBalance);
        String formattedBalance = String.format("%.2f", categoryBalance);
        balanceTextView.setText("Balance for " + valueType + ": " + formattedBalance);
        Log.d("BalanceUpdate", "Updated Balance for " + valueType + ": " + categoryBalance);
        userButton.performClick();
        giveButton.setVisibility(View.VISIBLE);
    }

    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(USER_ID_KEY, null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(USER_ID_KEY, userId);
            editor.apply();
        }

        return userId;
    }

    private void giveAmount() {
//        server_Close();
//        client_Close();
            startGiveProcess();

    }

    private void requestAmount() {

            startRequestProcess();
        }


    private ProcessCameraProvider cameraProvider;

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();  // Store cameraProvider for later use
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraExecutor.shutdown();
            cameraProvider.unbindAll();  // Stops the camera
        }
    }


    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            processImageProxy(imageProxy);
        });

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }


    private void startRequestProcess() {
        is_giver = false;
        is_btn_click = true;
        valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString().trim();
        if (valueToGive.isEmpty()) {
            Toast.makeText(this, "Please enter some value to give", Toast.LENGTH_SHORT).show();
            return;
        }
        if (valueType.equals("Select Type")) {
            Toast.makeText(this, "Please select any type", Toast.LENGTH_SHORT).show();
            return;
        }
        readButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.GONE);
        giveButton.setVisibility(View.GONE);
        quitButton.setVisibility(View.VISIBLE);
        quitButton.setEnabled(false);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        String qrData = "Type: " + valueType + ", Value: " + valueToGive + ", Timestamp: " + formattedDate + ", Status: " + "request" + ", ServerName: " + server_serverName + ", ServerIP: " + Utils.getIPAddress(true);
        generateQRCode(qrData);

//        quitButton.setEnabled(true);

        currentBalance = Stash.getFloat(valueType + "_balance", 0);
        currentBalance += Float.parseFloat(valueToGive);
        quitButton.setEnabled(true);
    }

    private void server_updateChatMessage(String str) {
        Handler server_handler = new Handler(Looper.getMainLooper());
        server_handler.post(() -> {
            if (str.equals("show_giver_confirmation")) {
                showAlertDialog();
            }
        });
    }

    class server_ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                server_serverSocket = new ServerSocket(55555);
                server_clientSocket = server_serverSocket.accept();
                // Accepts the client connection
                server_ps = new PrintStream(server_clientSocket.getOutputStream());
                server_br = new BufferedReader(new InputStreamReader(server_clientSocket.getInputStream()));
                server_ps.println("Hello from " + server_serverName + "!");
                server_updateChatMessage("Client connected!");
                while (true) {
                    String receivedMessage = server_br.readLine();
                    if (receivedMessage.equalsIgnoreCase("exit")) {
                        server_updateChatMessage("Client disconnected!");
                        break;
                    }
                    server_updateChatMessage(receivedMessage);
                }

            } catch (Exception e) {
                server_updateChatMessage("Error in connection");
                e.printStackTrace();
            }
        }
    }

    class server_SendThread implements Runnable {
        @Override
        public void run() {
            try {
                server_message = "yes_confirm";  // Clear the input field
                server_message = server_serverName + ": " + server_message;
                server_ps.println(server_message);  // Send message to client
                server_ps.flush();
                server_updateChatMessage(server_message);  // Update the server's chat display
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlertDialog() {
        confirmation_lyt.setVisibility(View.VISIBLE);
    }

    private void client_updateChatMessage(String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            Log.d("ClientActivity", "Received message: " + str); // Log the value of str
            if (str.equals("Server: yes_confirm")) {
                Log.d("ClientActivity", "Showing alert dialog");
                client_showAlertDialog();
            }
        });
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }


    private void processImageProxy(ImageProxy imageProxy) {
        @SuppressWarnings("ConstantConditions")
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {

                    for (Barcode barcode : barcodes) {
                        String qrCodeValue = barcode.getRawValue();

                        readButton.setVisibility(View.VISIBLE);
                        String[] parts = qrCodeValue.split(", ");
                        extractedType = "";
                        String extractedTimestamp = "";
                        status = "";
                        String extractedDevice;
                        for (String part : parts) {
                            if (part.startsWith("Type: ")) {
                                extractedType = part.substring("Type: ".length());
                            } else if (part.startsWith("Value: ")) {
                                extractedValue = part.substring("Value: ".length());
                            } else if (part.startsWith("Device: ")) {
                                extractedDevice = part.substring("Device: ".length());
                            } else if (part.startsWith("Timestamp: ")) {
                                extractedTimestamp = part.substring("Timestamp: ".length());
                            } else if (part.startsWith("Status: ")) {
                                status = part.substring("Status: ".length());
                            } else if (part.startsWith("ServerIP: ")) {
                                client_ip = part.substring("ServerIP: ".length());
                            } else if (part.startsWith("ServerName: ")) {
                                client_name = part.substring("ServerName: ".length());
                            }
                        }
                        cameraExecutor.shutdown();

//                        Toast.makeText(this, "IP " + client_ip, Toast.LENGTH_SHORT).show();
                        // Start communication with the server
                        Thread sc = new Thread(new client_StartCommunication());
                        sc.start();
                        if (status.equals("giver")) {

                            float balance = Stash.getFloat(extractedType + "_balance", 0);
                            if (balance != 0) {
                                currentBalance = balance;
                            } else {
                                currentBalance = 0f; // Default value
                                Log.d("BalanceCheck", "Balance was null, setting to default value of 0.");
                            }

                            currentBalance += Float.parseFloat(extractedValue);
//                            valueEditText.setText(extractedValue);
                            previewView.setVisibility(View.GONE);
                            System.out.println("Timestamp: " + status);
                            Stash.put("type", "reader");
                        } else {
                             float balance = Stash.getFloat(extractedType + "_balance", 0);
                            if (balance != 0) {
                                currentBalance = balance;
                            } else {
                                currentBalance = Float.valueOf(0); // Default value or handle appropriately
                            }
                            if (Float.parseFloat(extractedValue)>=currentBalance) {
                                Toast.makeText(this, "Insufficient balance for transaction", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            currentBalance -= Float.parseFloat(extractedValue);
                            System.out.println("Timestamp: " + extractedTimestamp);
                            previewView.setVisibility(View.GONE);

                            Stash.put("type", "reader");
                        }
                        break;

                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "eror" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                })
                .addOnCompleteListener(task -> imageProxy.close()).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "eror" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class client_StartCommunication implements Runnable {

        @Override
        public void run() {
            try {
                InetSocketAddress inetAddress = new InetSocketAddress(client_ip, 55555);
                client_clientSocket = new Socket();
                client_clientSocket.connect(inetAddress, 7000);
                client_ps = new PrintStream(client_clientSocket.getOutputStream());
                client_updateChatMessage("Connected to " + client_ip + " !!\n");
                client_ps.println("j01ne6:" + client_name);
                BufferedReader br = new BufferedReader(new InputStreamReader(client_clientSocket.getInputStream()));
                client_ps.println("show_giver_confirmation");

                while (true) {
                    final String str = br.readLine();
                    if (str.equalsIgnoreCase("exit")) {
                        client_updateChatMessage("Server Closed the Connection!");
                        Thread.sleep(2000);
                        finish();
                        break;
                    }
                    client_updateChatMessage(str);
                }
            } catch (final Exception e) {
                client_updateChatMessage("Not able to connect!" + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (Exception ignored) {
                }
                finish();
            }
        }
    }


    private void client_showAlertDialog() {
        Stash.put(extractedType + "_balance", currentBalance);
        // Format categoryBalance to 2 decimal places
        String formattedBalance = String.format("%.2f", currentBalance);
        MainActivity.balanceTextView.setText("Balance for " + extractedType + ": " + formattedBalance);
        setSpinnerToValue(MainActivity.typeSpinner, extractedType);
        MainActivity.valueEditText.setText("");
    }

}
