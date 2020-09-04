package com.example.googlecloudvision;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Vision vision;
    Bitmap bitmap;
    Canvas canvas;
    ImageView imagen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(),  null);
        visionBuilder.setVisionRequestInitializer(new
                VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        vision = visionBuilder.build();
    }

    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION",
                        getImageToProcess());
                try {

                    FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                            .setTrackingEnabled(false).build();

                    Vision.Images.Annotate  annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response  = annotateRequest.execute();
                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces = faces.size();
                    String likelihoods = "";

                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<Face> face = faceDetector.detect(frame);

                        for(int i=0; i<numberOfFaces; i++){
                            likelihoods += "\n Rostro " + i + "  "  +
                                    faces.get(i).getJoyLikelihood();
                            detectedResponse(bitmap, face);
                        }

                    final String message =   "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imagen.setImageDrawable(new BitmapDrawable(getResources(),bitmap));
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.txtResult);
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void detectedResponse(Bitmap mbitmap, SparseArray<Face> mfaces) {

        canvas = new Canvas(bitmap);
        canvas.drawBitmap(mbitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);

        for(int i=0; i<mfaces.size(); i++) {
            Face face = mfaces.valueAt(i);
            int x = (int) face.getPosition().x;
            int y = (int) face.getPosition().y;
            int width = (int) face.getWidth() + x;
            int height = (int) face.getHeight() + y;
            canvas.drawRect(x, y, width, height, paint);
        }
    }

    public Image getImageToProcess(){
        imagen = (ImageView)findViewById(R.id.imgImgToProcess);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        bitmap = drawable.getBitmap();
        bitmap = scaleBitmapDown(bitmap, 1200);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageInByte = stream.toByteArray();
        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);

        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));

        BatchAnnotateImagesRequest batchRequest =  new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }
}
