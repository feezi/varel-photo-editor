package com.varel.photo_editor.abstract_libs;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.varel.photo_editor.R;
import com.varel.photo_editor.libs.ActionBarLib;
import com.varel.photo_editor.libs.ImageFilters;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public abstract class ImageFragment extends Fragment {

   public ActionBar actionBar;
   private int imageWidth;
   private int imageHeight;
   protected Bitmap imageBitmap;
   protected Bitmap imageBitmap_;
   protected Bitmap imageBitmap_active;
   protected ImageFilters imageFilters = new ImageFilters();
   protected List<Bitmap> imagesEffectSave;
   protected List<Integer> imagesEffectSaveIndex;
   protected ImageView imageView;

   protected boolean isAddImage = false;
   protected boolean isBorder = false;
   protected boolean isGaussian = false;
   protected boolean isLight = false;
   protected int isActiveFilters = 0;
   protected float isRotate = 0;
   protected String folderApplication = Environment.getExternalStorageDirectory() + "/PhotoEditor/";
   protected int MAX_WIDTH_IMAGE = 1024;
   protected int MAX_HEIGHT_IMAGE = 1024;

   protected int TYPE_RESIZE_IMAGE = 0;
   final public int RESIZE_IMAGE_NONE = 0;
   final public int RESIZE_IMAGE_SQUARE = 1;

   protected void updateImageToView(Bitmap pImageBitmap) {
      if(pImageBitmap != null) {
         if(isGaussian) {
            double[][] GaussianBlurConfig = new double[][] {
                    { 2, 2, 2 },
                    { 2, 0, 2 },
                    { 2, 2, 2 }
            };
            pImageBitmap = imageFilters.applyGaussianBlurEffect(pImageBitmap, GaussianBlurConfig);
         }

         if(isLight) {
            pImageBitmap = imageFilters.applyBrightnessEffect(pImageBitmap, 70);
         }

         if(isRotate != 0) {
            pImageBitmap = imageFilters.applyRotateEffect(pImageBitmap, isRotate);
         }

         if(isBorder) {
            pImageBitmap = imageFilters.applySimpleBorder(pImageBitmap, getActivity(), 0.1f);
         }
      }
      imageBitmap_active = pImageBitmap;
   }

   protected void saveImage() {
      if(isAddImage) {
         new ApplyFiltersTasks("Сохранение фотографии") {
            protected void doingInBackground() {
               try {
                  String name = "file_" + (new DateFormat().format("s_m_k_dd_MM_yyyy", new Date()).toString()) + ".png";
                  File filename = new File(folderApplication, name);
                  FileOutputStream out = new FileOutputStream(filename);
                  Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                  bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                  out.close();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
            protected void doingAfter() {
               Toast.makeText(getActivity(), "Фотография сохранена.", Toast.LENGTH_SHORT).show();
            }
         }.execute();
      }
   }

   protected void setImage() {
      if(imageBitmap_active !=  null) {
         switch (TYPE_RESIZE_IMAGE) {
            case RESIZE_IMAGE_NONE:
               break;

            case RESIZE_IMAGE_SQUARE:
               if(imageHeight != imageWidth) {
                  int newSize = imageHeight > imageWidth ? imageHeight : imageWidth;
                  Bitmap newBitmap = Bitmap.createBitmap(newSize, newSize, Bitmap.Config.ARGB_8888);
                  Canvas canvas = new Canvas(newBitmap);
                  canvas.drawColor(Color.WHITE);
                  canvas.drawBitmap(imageBitmap, (int) ((newSize - imageWidth) / 2), (int) ((newSize - imageHeight) / 2), null);
                  imageHeight = imageWidth = newSize;
                  imageBitmap_ = imageBitmap_active = imageBitmap = newBitmap;
               }
               break;
         }
         isAddImage = true;
         imageView.setImageBitmap(imageBitmap_active);
      }
   }

   protected Bitmap reSizeImage(Bitmap image, int width, int height) {
      return Bitmap.createScaledBitmap(image, width, height, true);
   }

   protected Bitmap reSizeImage(Bitmap image) {
      int height = image.getHeight();
      int width = image.getWidth();
      if(height > MAX_HEIGHT_IMAGE || width > MAX_WIDTH_IMAGE) {
         float koef = (float) height / (float) width;
         int newWidth;
         int newHeight;
         if(height > width) {
            newHeight = MAX_HEIGHT_IMAGE;
            newWidth = (int) (newHeight / koef);
         } else {
            newWidth = MAX_WIDTH_IMAGE;
            newHeight = (int) (newWidth * koef);
         }
         imageHeight = newHeight;
         imageWidth = newWidth;
         return reSizeImage(image, newWidth, newHeight);
      } else {
         imageHeight = height;
         imageWidth = width;
         return image;
      }
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
      super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
      imagesEffectSave = new ArrayList<Bitmap>();
      imagesEffectSaveIndex = new ArrayList<Integer>();
      switch(requestCode) {
         case 0:
            if (resultCode == Activity.RESULT_OK) {
               Bundle extras = imageReturnedIntent.getExtras();
               imageBitmap_ = imageBitmap = reSizeImage((Bitmap) extras.get("data"));
               updateImageToView(imageBitmap_);
               setImage();
            }

            break;

         case 1:
            if(resultCode == Activity.RESULT_OK){
               Uri selectedImage = imageReturnedIntent.getData();
               String[] filePathColumn = {MediaStore.Images.Media.DATA};
               Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
               cursor.moveToFirst();
               int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
               String filePath = cursor.getString(columnIndex);
               cursor.close();

               imageBitmap_ = imageBitmap = reSizeImage(BitmapFactory.decodeFile(filePath));
               updateImageToView(imageBitmap_);
               setImage();
            }
            break;
      }
   }

   protected abstract class ApplyFiltersTasks extends AsyncTasks {
      private ProgressDialog dialog;
      protected void doingInBackground() {}
      protected void doingAfter() {}
      protected void doingBefore() {}
      private String textShow = "";
      private Context context;

      public ApplyFiltersTasks() {
         context = getActivity();
         textShow = context.getString(R.string.loader_text_wait);
      }

      public ApplyFiltersTasks(String strWait) {
         context = getActivity();
         textShow = strWait;
      }

      @Override
      protected void onPreExecute() {
         doingBefore();
         dialog = ProgressDialog.show(context, "", textShow, true);
      }

      @Override
      protected void onPostExecute(String result) {
         dialog.dismiss();
         setImage();
         doingAfter();
      }
   }
}
