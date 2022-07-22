package com.gisfy.ntfp.VSS.Collectors;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gisfy.ntfp.ExcelImport.ExcelUtil;
import com.gisfy.ntfp.R;
import com.gisfy.ntfp.SqliteHelper.DBHelper;
import com.gisfy.ntfp.Utils.RealPathUtil;
import com.gisfy.ntfp.Utils.SnackBarUtils;
import com.gisfy.ntfp.Utils.StaticChecks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ImportExcel extends AppCompatActivity {
    public static final String TAG = "ImportExcelTag";
    private Context mContext;
    private final int FILE_SELECTOR_CODE = 10000;
    private final List<Map<Integer, Object>> readExcelList = new ArrayList<>();

    private ImageView retake;
    private TextView uploadtv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_excel);
        mContext = this;

        initViews();

        findViewById(R.id.idcardCardTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStoragePermissionGranted()) {
                    openFileSelector();
                } else {
                    SnackBarUtils.ErrorSnack(ImportExcel.this,getString(R.string.storagepermission));
                }
            }
        });

        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStoragePermissionGranted()) {
                    openFileSelector();
                } else {
                    SnackBarUtils.ErrorSnack(ImportExcel.this,getString(R.string.storagepermission));
                }
            }
        });
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initViews() {
        StaticChecks checks = new StaticChecks(this);
        uploadtv = findViewById(R.id.idcardCardTV);
        retake = findViewById(R.id.idcardCard2);
    }



    public boolean isStoragePermissionGranted() {
        String TAG = "Permisiion";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                return false;
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        startActivityForResult(intent, FILE_SELECTOR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri == null) return;
            Log.i(TAG, "onActivityResult: " + "filePath：" + uri.getPath());
            uploadtv.setText(getResources().getText(R.string.uploadagain));
            importExcelDeal(uri);

        }
    }

    private void importExcelDeal(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "doInBackground: Importing...");
                ImportExcel.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.spin_kit).setVisibility(View.VISIBLE);
                        SnackBarUtils.SuccessSnack(ImportExcel.this,getString(R.string.importing));
                    }
                });

                final List<Map<Integer, Object>> readExcelNew = ExcelUtil.readExcelNew(mContext, uri, RealPathUtil.getRealPath(ImportExcel.this,uri));

                Log.i(TAG, "onActivityResult:readExcelNew " + ((readExcelNew != null) ? readExcelNew.size() : ""));

                if (readExcelNew != null && readExcelNew.size() > 0) {
                    readExcelList.clear();
                    readExcelList.addAll(readExcelNew);
                    Log.i(TAG, "run: successfully imported");
                    ImportExcel.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.spin_kit).setVisibility(View.GONE);
                            new addtoDB(readExcelNew).execute();
                            SnackBarUtils.SuccessSnack(ImportExcel.this,getString(R.string.importsuccess));
                        }
                    });
                } else {
                    ImportExcel.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.spin_kit).setVisibility(View.GONE);
                            SnackBarUtils.ErrorSnack(ImportExcel.this,getString(R.string.excelerror));
                        }
                    });
                }
            }
        }).start();
    }


    private class addtoDB extends AsyncTask<String,String,String>{

        List<Map<Integer, Object>> readExcelNew;
        private addtoDB(List<Map<Integer, Object>> readExcelNew ){
            this.readExcelNew=readExcelNew;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.spin_kit).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            for (Map<Integer, Object> map: readExcelNew ){
                String name="",village="",div="",range="",vss="",spouse="",cat="",age="",dob="",idcard="",idnum="",
                edu="",familycnt="",accnum="",bank="",ifsc="",remarks="";
                for (int i=0;i<18;i++){
                    if (map.get(i)!=null) {
                        switch (i) {
                            case 0:
                                name = map.get(i).toString();
                                break;
                            case 1:
                                village = map.get(i).toString();
                                break;
                            case 2:
                                div = map.get(i).toString();
                                break;
                            case 3:
                                range = map.get(i).toString();
                                break;
                            case 4:
                                vss = map.get(i).toString();
                                break;
                            case 5:
                                spouse = map.get(i).toString();
                                break;
                            case 6:
                                cat = map.get(i).toString();
                                break;
                            case 7:
                                age = map.get(i).toString();
                                break;
                            case 8:
                                dob = map.get(i).toString();
                                break;
                            case 9:
                                idcard = map.get(i).toString();
                                break;
                            case 10:
                                idnum = map.get(i).toString();
                                break;
                            case 11:
                                edu = map.get(i).toString();
                                break;
                            case 12:
                                familycnt = map.get(i).toString();
                                break;
                            case 13:
                                accnum = map.get(i).toString();
                                break;
                            case 14:
                                bank = map.get(i).toString();
                                break;
                            case 15:
                                ifsc = map.get(i).toString();
                                break;
                            case 16:
                                remarks = map.get(i).toString();
                                break;
                        }
                    }
                }
                UUID uuid=UUID.randomUUID();
                String usernamestr;
                try {
                    usernamestr=div.substring(0,3).toUpperCase()+"-"+range.substring(0,3).toUpperCase()+"-"+vss.substring(0,3).toUpperCase()+"-"+uuid.toString().substring(0,3).toUpperCase();
                    Collector model=new Collector(uuid.toString(),vss,div,range,village,name,spouse,cat,age,"",dob,idcard,idnum,"",edu,
                            familycnt,bank,accnum,ifsc,usernamestr,"pass@123",remarks,0);
                    DBHelper db=new DBHelper(ImportExcel.this);
                    db.insertData(model);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            findViewById(R.id.spin_kit).setVisibility(View.GONE);
        }
    }
}
