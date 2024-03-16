package com.example.exavera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.Manifest;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.exavera.ml.Paises;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import com.squareup.picasso.Picasso;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnSuccessListener<Text>, OnFailureListener {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    Bitmap mSelectedImage;

    ImageView mImageView, flagImageView;

    TextView txtResults, paistexto;

    private GoogleMap map;

    private Map<String, String> countryCodeToName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResults = findViewById(R.id.txtresults);
        paistexto = findViewById(R.id.paistexto);
        mImageView = findViewById(R.id.image_view);
        flagImageView= findViewById(R.id.flagImageView);

        initCountryCodeToNameMap();

        if(checkSelfPermission(android.Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
            requestPermissions(new
                    String[]{Manifest.permission.CAMERA},100);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
            }
        });
    }

    private void initCountryCodeToNameMap() {
        countryCodeToName = new HashMap<>();
        countryCodeToName.put("AR", "Argentina");
        countryCodeToName.put("BE", "Bélgica");
        countryCodeToName.put("BR", "Brasil");
        countryCodeToName.put("CO", "Colombia");
        countryCodeToName.put("CR", "Croacia");
        countryCodeToName.put("EC", "Ecuador");
        countryCodeToName.put("ES", "España");
        countryCodeToName.put("FR", "Francia");
        countryCodeToName.put("GB", "Inglaterra");
        countryCodeToName.put("MX", "México");
        countryCodeToName.put("PT", "Portugal");
        countryCodeToName.put("SE", "Suecia");
        countryCodeToName.put("UY", "Uruguay");
    }

    public void abrirGaleria (View view){
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamara (View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage =
                            MediaStore.Images.Media.getBitmap(getContentResolver(),
                                    data.getData());
                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void OCRfx(View v) {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados="";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        }else{
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados = resultados + elements.get(k).getText() + " ";
                    }
                }
            }
            resultados=resultados + "\n";
        }
        txtResults.setText(resultados);
    }
    public void PersonalizedModel(View v) {
        try {
            String[] etiquetas = {"AR", "BE", "BR", "CO", "CR", "EC", "ES", "FR", "GB", "MX", "PT", "SE", "UY"};
            Paises model = Paises.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(convertirImagenATensorBuffer(mSelectedImage));
            Paises.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            String codigoISO2 = obtenerEtiquetayProbabilidad(etiquetas, outputFeature0.getFloatArray());
            paistexto.setText(obtenerNombre(etiquetas, outputFeature0.getFloatArray()));
            model.close();
            obtenerInfoPais(codigoISO2);
        } catch (Exception e) {
            txtResults.setText(e.getMessage());
        }
    }
    public ByteBuffer convertirImagenATensorBuffer(Bitmap mSelectedImage){
        Bitmap imagen = Bitmap.createScaledBitmap(mSelectedImage, 224, 224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        imagen.getPixels(intValues, 0, imagen.getWidth(), 0, 0, imagen.getWidth(), imagen.getHeight());
        int pixel = 0;
        for(int i = 0; i < imagen.getHeight(); i ++){
            for(int j = 0; j < imagen.getWidth(); j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }
    public String obtenerEtiquetayProbabilidad(String[] etiquetas, float[] probabilidades){
        float valorMayor=Float.MIN_VALUE;
        int pos=-1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        return etiquetas[pos];
    }

    public String obtenerNombre(String[] etiquetas, float[] probabilidades){
        float valorMayor=Float.MIN_VALUE;
        int pos=-1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        String countryCode = etiquetas[pos];
        String countryName = countryCodeToName.get(countryCode);
        return countryName != null ? countryName : "País Desconocido";
    }

    public void obtenerInfoPais(String codigoISO2) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://www.geognos.com/api/en/countries/info/" + codigoISO2 + ".json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String responseData = response.body().string();

                // Parsear JSON
                Gson gson = new Gson();
                GeoInfoResponse geoInfoResponse = gson.fromJson(responseData, GeoInfoResponse.class);

                // Actualizar la interfaz de usuario en el hilo principal
                runOnUiThread(() -> mostrarInfoPais(geoInfoResponse));
            }
        });
    }

    public void mostrarInfoPais(GeoInfoResponse geoInfoResponse) {
        if (geoInfoResponse.getStatusMsg().equals("OK")) {
            GeoInfoResponse.ResultsBean results = geoInfoResponse.getResults();

            // Obtener las coordenadas del rectángulo
            GeoInfoResponse.ResultsBean.GeoRectangleBean rectangle = results.getGeoRectangle();

            // Crear los límites del rectángulo
            LatLngBounds bounds = new LatLngBounds(
                    new LatLng(rectangle.getSouth(), rectangle.getWest()), // Suroeste
                    new LatLng(rectangle.getNorth(), rectangle.getEast())  // Noreste
            );

            // Dibujar el rectángulo en el mapa
            map.addPolygon(new PolygonOptions()
                    .add(new LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
                            new LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                            new LatLng(bounds.northeast.latitude, bounds.northeast.longitude),
                            new LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                            new LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
                    .strokeColor(Color.RED)
                    .fillColor(Color.TRANSPARENT));

            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));

            String capital = results.getCapital().getName();
            String iso2 = results.getCountryCodes().getIso2();
            String iso3 = results.getCountryCodes().getIso3();
            String fips = results.getCountryCodes().getFips();
            String telPrefix = results.getTelPref();
            String center = results.getGeoPt().toString();

            // Construir la cadena con la información del país
            String infoPais = "Capital: " + capital + "\n" +
                    "ISO 2: " + iso2 + "\n" +
                    "ISO 3: " + iso3 + "\n" +
                    "FIPS: " + fips + "\n" +
                    "Tel Prefix: " + telPrefix + "\n" +
                    "Center: " + center + "\n" +
                    "Rectangle: " + rectangle.getWest() + " " + rectangle.getEast() + " " + rectangle.getNorth() + " " + rectangle.getSouth();


            // Mostrar la información en el TextView
            txtResults.setText(infoPais);

            // Cargar la bandera del país usando Picasso
            String flagUrl = "http://www.geognos.com/api/en/countries/flag/" + iso2 + ".png";
            Picasso.get().load(flagUrl).into(flagImageView);
        }
    }
}