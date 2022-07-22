package com.gisfy.ntfp.VSS.Inventory;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gisfy.ntfp.HomePage.Home;
import com.gisfy.ntfp.Login.Models.VSSUser;
import com.gisfy.ntfp.R;
import com.gisfy.ntfp.SqliteHelper.DBHelper;
import com.gisfy.ntfp.SqliteHelper.Entity.InventoryRelation;
import com.gisfy.ntfp.SqliteHelper.NtfpDao;
import com.gisfy.ntfp.SqliteHelper.SynchroniseDatabase;
import com.gisfy.ntfp.Utils.SharedPref;
import com.gisfy.ntfp.Utils.SnackBarUtils;
import com.gisfy.ntfp.Utils.StaticChecks;
import com.gisfy.ntfp.VSS.Collectors.Collector;
import com.gisfy.ntfp.VSS.Collectors.list_collectors;
import com.gisfy.ntfp.VSS.Shipment.Model_shipment;
import com.github.ybq.android.spinkit.SpinKitView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.gisfy.ntfp.Utils.StaticChecks.stringtoDate;

public class list_inventory  extends AppCompatActivity {

    private RecyclerView recyclerView;
    private adapter_inventory adapter;
    public boolean isinActionMode=false;
    private List<InventoryRelation> list=new ArrayList<>();
    public ImageView sync,upload;
    private SpinKitView spinKitView;
    private boolean btnFlag=false;
    private NtfpDao dao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_collectors);
        intiViews();
        dao = SynchroniseDatabase.getInstance(this).ntfpDao();
        new fetchTask().execute();
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        new StaticChecks(list_inventory.this).showSnackBar("Filtered...");
                    }
                }, mYear, mMonth, mDay);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnFlag)
                    SnackBarUtils.WarningSnack(list_inventory.this,getString(R.string.wait));
                else {
                    if (adapter.getSelectedItems().size()>0){
                        new uploadTask().execute();
                    }else{
                        SnackBarUtils.ErrorSnack(list_inventory.this,"No Items Selected");
                    }
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
    @Override
    public void onBackPressed() {
        if (isinActionMode) {
            isinActionMode = false;
            adapter.notifyDataSetChanged();
        }else {
            Intent i=new Intent(this,Home.class);
            i.putExtra("position",2);
            i.putExtra("title",getString(R.string.inventory));
            startActivity(i);
        }
    }
    private void intiViews() {
        TextView title = findViewById(R.id.titlebar_title);
        spinKitView=findViewById(R.id.spin_kit);
        title.setText(R.string.listinv);
        recyclerView=findViewById(R.id.recyclerView);
        sync=findViewById(R.id.synchronise);
        upload=findViewById(R.id.upload);
        list_inventory.this.setFinishOnTouchOutside(true);
        title.setText(getString(R.string.listinv));
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload.setVisibility(View.VISIBLE);
                sync.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload.setVisibility(View.GONE);
                sync.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }



    private class fetchTask extends AsyncTask<String,String,String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            spinKitView.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                list= dao.getAllInventories();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return getString(R.string.somethingwentwrong);
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            adapter=new adapter_inventory(list,list_inventory.this);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerView.setLayoutManager(layoutManager);
            if (list.size()>0)
                Collections.sort(list,new SortByDate());
            recyclerView.setAdapter(adapter);
            if (s!=null)
                SnackBarUtils.ErrorSnack(list_inventory.this,s);
            spinKitView.setVisibility(View.GONE);
        }
    }
    private class uploadTask extends AsyncTask<String,String,String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            spinKitView.setVisibility(View.VISIBLE);
            btnFlag=true;
        }

        @Override
        protected String doInBackground(String... strings) {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");
            Log.i("jsonData188",jsonwriter());
            RequestBody body = RequestBody.create(mediaType,jsonwriter());
            Request request = new Request.Builder()
                    .url("http://13.127.166.242/NTFPAPI/API/Stocks")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (setStatus(response.body().string().trim()))
                    return response.message();
                else
                    return getResources().getString(R.string.somedetailsnotsynced);
            }catch (Exception e) {
                e.printStackTrace();
                btnFlag=false;
                return e.getClass().getSimpleName();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            btnFlag=false;
            if (s.equals("OK"))
                SnackBarUtils.SuccessSnack(list_inventory.this,getString(R.string.synced));
            else if(s.equals(getResources().getString(R.string.somedetailsnotsynced)))
                SnackBarUtils.WarningSnack(list_inventory.this,s);
            else
                SnackBarUtils.ErrorSnack(list_inventory.this,getResources().getString(R.string.servernotresponding));
            findViewById(R.id.spin_kit).setVisibility(View.GONE);
            isinActionMode=false;
            upload.setVisibility(View.GONE);
            sync.setVisibility(View.VISIBLE);
            if (list.size()>0)
                Collections.sort(list,new SortByDate());
            adapter.notifyDataSetChanged();
            super.onPostExecute(s);
        }
    }
    private String jsonwriter(){
        List<String> newobjects=new ArrayList<>();
        VSSUser user=new SharedPref(this).getVSS();
        for (InventoryRelation model:adapter.getSelectedItems()){
            if (model.isSelected()&&!model.getInventory().isSynced()){
                int memberId;
                if (model.getMember()!=null) memberId = model.getMember().getMemberId();
                else memberId = -1;

                newobjects.add("{\n" +
                        "    \"Random\":\""+model.getInventory().getInventoryId()+"\",\n" +
                        "    \"DivisionId\":\""+user.getDivisionId()+"\",\n" +
                        "    \"RangeId\":\""+user.getRangeId()+"\",\n" +
                        "    \"VSSId\":\""+user.getVid()+"\",\n" +
                        "    \"NTFPName\":\""+model.getNtfp().getNTFPscientificname()+"\",\n" +
                        "    \"NTFPId\":\""+model.getNtfp().getNid()+"\",\n" +
                        "    \"MemberId\":\""+memberId+"\",\n" +
                        "    \"CollectorId\":\""+model.getCollector().getCid()+"\",\n" +
                        "    \"NTFPTypeId\":\""+model.getItemType().getItemId()+"\",\n" +
                        "    \"Unit\":\""+model.getInventory().getMeasurements()+"\",\n" +
                        "    \"Quantity\":\""+model.getInventory().getQuantity()+"\",\n" +
                        "    \"NTFPType\":\""+model.getItemType().getMycase()+"\",\n" +
                        "    \"Collector\":\""+model.getCollector().getCollectorName()+"\",\n" +
                        "    \"Amount\":\""+model.getInventory().getPrice()+"\",\n" +
                        "    \"DateandTime\":\""+model.getInventory().getDate()+"\"\n" +
                        "}");
            }
        }
        StringBuilder sb=new StringBuilder("[");
        if (newobjects.size()>0){
            for (int i=0;i<newobjects.size();i++){
                if (i==newobjects.size()-1){
                    sb.append(newobjects.get(i));
                }else {
                    sb.append(newobjects.get(i)).append(",");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    private boolean setStatus(String json) throws JSONException {
        boolean flag=true;
        Log.i("kishore",json);
        JSONArray jsonArray=new JSONArray(json);
        for (int i=0;i<jsonArray.length();i++){
            JSONObject details=jsonArray.getJSONObject(i);
            if (details.getString("Status").equals("Success")) {
                dao.setSyncStatus(true,details.getString("Random"));
            }else{
                flag=false;
                dao.setSyncStatus(false,details.getString("Random"));
            }
        }
        list.clear();
        list.addAll(dao.getAllInventories());
        return flag;
    }
    static class SortByDate implements Comparator<InventoryRelation> {
        @Override
        public int compare(InventoryRelation a, InventoryRelation b) {
            return Boolean.compare(a.getInventory().isSynced(), b.getInventory().isSynced());
        }
    }

}