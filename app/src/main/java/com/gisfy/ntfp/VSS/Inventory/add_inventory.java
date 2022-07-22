package com.gisfy.ntfp.VSS.Inventory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gisfy.ntfp.Collectors.CollectorStockModel;
import com.gisfy.ntfp.Login.Models.VSSUser;
import com.gisfy.ntfp.R;
import com.gisfy.ntfp.SqliteHelper.DBHelper;
import com.gisfy.ntfp.SqliteHelper.Entity.CollectorsModel;
import com.gisfy.ntfp.SqliteHelper.Entity.InventoryEntity;
import com.gisfy.ntfp.SqliteHelper.Entity.InventoryRelation;
import com.gisfy.ntfp.SqliteHelper.Entity.MemberModel;
import com.gisfy.ntfp.SqliteHelper.Entity.NTFP;
import com.gisfy.ntfp.SqliteHelper.NtfpDao;
import com.gisfy.ntfp.SqliteHelper.SynchroniseDatabase;
import com.gisfy.ntfp.Utils.FCMNotifications;
import com.gisfy.ntfp.Utils.SharedPref;
import com.gisfy.ntfp.Utils.SnackBarUtils;
import com.gisfy.ntfp.Utils.StaticChecks;
import com.gisfy.ntfp.VSS.CollectorInventory.CollectorInventory;
import com.gisfy.ntfp.VSS.Payment.Model_payment;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class add_inventory extends AppCompatActivity {
    private TextInputEditText vss,division,range,quantity, amountPaid;
    private TextView date;
    private Spinner ntfpgrade,measurement;
    private AutoCompleteTextView ntfptype,member,products,collector;
    private Button proceed;
    private StaticChecks check;
    private StaticChecks checks;
    private NTFP ntfpModel=null;
    private CollectorsModel collectorModel = null;
    private NTFP.ItemType itemTypeModel =null;
    private MemberModel memberModel = null;
    private ArrayAdapter<NTFP.ItemType> ntfpTypeAdapter = null;
    private ArrayAdapter<MemberModel> memberAdapter=null;
    boolean spinnerflag = false;

    private DBHelper db;
    private String inventoryId="";
    private NtfpDao dao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory);
        intiViews();
        dao = SynchroniseDatabase.getInstance(this).ntfpDao();
        db=new DBHelper(this);
        if (getIntent().hasExtra("uid")) {
            proceed.setText(getString(R.string.update));
            inventoryId = getIntent().getStringExtra("uid");
            InventoryRelation relation = dao.getInventoryFromId(inventoryId);
            if (relation.getItemType()!=null){
                ntfptype.setText(relation.getItemType().getMycase(),false);
            }
            itemTypeModel = relation.getItemType();
            if (relation.getInventory().getGrade()!=null){
                switch (relation.getInventory().getGrade()) {
                    case "Grade1":
                        ntfpgrade.setSelection(1);
                        break;
                    case "Grade2":
                        ntfpgrade.setSelection(2);
                        break;
                    case "Grade3":
                        ntfpgrade.setSelection(3);
                        break;
                    default:
                        ntfpgrade.setSelection(0);
                        break;
                }
            }
            products.setText(relation.getNtfp()+"",false);
            ntfpModel = relation.getNtfp();
            String MemberName;
            if (relation.getMember()!=null)MemberName = relation.getMember().getName();
            else MemberName = "";
            member.setText(MemberName,false);
            memberModel = relation.getMember();
            collector.setText(relation.getCollector().getCollectorName(),false);
            collectorModel = relation.getCollector();
            amountPaid.setText(String.valueOf(relation.getInventory().getPrice()));
            quantity.setText(String.valueOf(relation.getInventory().getQuantity()));
Log.i("measurementUnit121",relation.getInventory().getMeasurements()+"");
            if (relation.getInventory().getMeasurements().equals("kilogram"))
                measurement.setSelection(1);
            else if(relation.getInventory().getMeasurements().equals("Gram"))
                measurement.setSelection(2);
            else
                measurement.setSelection(0);
        }
        else if(getIntent().hasExtra("CollectorModel")){
            CollectorStockModel model = (CollectorStockModel) getIntent().getSerializableExtra("CollectorModel");
            ntfptype.setText(model.getnTFPType(),false);
            itemTypeModel = dao.getAllNTFPTypes().get(model.getnTFPTypeId());
            ntfpModel = dao.getNTFPFromNId(model.getnTFPId());
            products.setText(ntfpModel.getNTFPmalayalamname()+"( " +ntfpModel.getNTFPscientificname() + " )",false);
            memberModel = dao.getMemberFromMemberId(model.getMemberId());
            member.setText(model.getmName());
            member.setEnabled(false);
            quantity.setText(model.getQuantity());
            inventoryId = model.getRandom();
            date.setText(model.getDateTime());
            collectorModel = dao.getCollectorFromCId(model.getCollectorID());
            collector.setText(model.getCollectorName(),true);
            collector.setEnabled(false);
            ntfpTypeAdapter = new ArrayAdapter<>(this, R.layout.multiline_spinner_dropdown_item, dao.getItemTypesFromNid(model.getnTFPId()));
            ntfptype.setAdapter(ntfpTypeAdapter);
            measurementSpinner(measurement, model.getUnit());
            gradeSpinner(ntfpgrade, "Grade1");

            double basePrice;
            if (ntfpgrade.getSelectedItemPosition()==1)
                basePrice = itemTypeModel.getGrade1Price();
            else if (ntfpgrade.getSelectedItemPosition() == 2)
                basePrice = itemTypeModel.getGrade2Price();
            else
                basePrice = itemTypeModel.getGrade3Price();

            if (quantity.getText().length()>0)
                amountPaid.setText(String.valueOf(Double.parseDouble(quantity.getText().toString())*basePrice));
            else
                amountPaid.setText("");

        }
        else{
            Date d = new Date();
            inventoryId = "TRANS-"+new SharedPref(add_inventory.this).getVSS().getvSSName().substring(0,3).toUpperCase()+"-" + d.getDay() + d.getMonth() + d.getMinutes() + d.getSeconds();
        }

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (collectorModel!=null ){
                    if (itemTypeModel!=null ) {
                        if (check.checkSpinnerList(new Spinner[]{ntfpgrade,measurement}))
                            if (checks.checkETList(new TextInputEditText[]{quantity})) {
                                try {
                                    Date d = new Date();
                                    int memberId;
                                    if (memberModel != null)
                                        memberId = memberModel.getMemberId();
                                    else
                                        memberId = 0;
                                    Log.i("getNtfpId173", ntfpModel.getNid() + "");
                                    InventoryEntity entity = new InventoryEntity(
                                            inventoryId,
                                            collectorModel.getCid(),
                                            memberId,
                                            ntfpModel.getNid(),
                                            ntfpModel.getNid(),
                                            ntfpgrade.getSelectedItem().toString(),
                                            measurement.getSelectedItem().toString(),
                                            Double.parseDouble(quantity.getText().toString()),
                                            Double.parseDouble(amountPaid.getText().toString()),
                                            date.getText().toString());
                                    VSSUser user = new SharedPref(add_inventory.this).getVSS();
                                    Model_payment payment = new Model_payment(
                                            inventoryId,
                                            user.getvSSName(),
                                            user.getDivisionname(),
                                            user.getRangeName(),
                                            amountPaid.getText().toString(),
                                            date.getText().toString(),
                                            "",
                                            "",
                                            "Made",
                                            collectorModel.getCollectorName(),
                                            ntfpModel.getNTFPmalayalamname()+"("+ntfpModel.getNTFPscientificname()+")",
                                            measurement.getSelectedItem().toString(),
                                            quantity.getText().toString(),
                                            0,
                                            0);
                                    db.insertData(payment);
                                    if (getIntent().hasExtra("")) {
                                        String body = "{\n" +
                                                "\t\"InventID\":\"" + inventoryId + "\",\n" +
                                                "\t\"VSSStatus\":\"true\"\n" +
                                                "}";
                                        new uploadTask(entity, payment).execute(body);
                                    } else {
                                        dao.insertInventory(entity);
                                        startActivity(new Intent(add_inventory.this, list_inventory.class));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    SnackBarUtils.ErrorSnack(add_inventory.this, getString(R.string.somethingwentwrong));
                                }
                            }
                    }else {
                        Toast.makeText(add_inventory.this, "please select NTFP Names and NTFP Type", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(add_inventory.this, "please select Collector", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.basicdetails_layout).getVisibility()==View.VISIBLE){
                    findViewById(R.id.basicdetails_layout).setVisibility(View.GONE);
                }else{
                    findViewById(R.id.basicdetails_layout).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void intiViews() {
        check = new StaticChecks(this);
        collector = findViewById(R.id.collector_spinner);
        checks=new StaticChecks(this);
        member=findViewById(R.id.spinner_member);
        ntfptype=findViewById(R.id.ntfptype);
        ntfpgrade=findViewById(R.id.ntfpgrade);
        quantity=findViewById(R.id.edit_quantity);
        amountPaid= findViewById(R.id.amountPaid);
        date=findViewById(R.id.date);
        products=findViewById(R.id.spinner_ntfps);
        proceed=findViewById(R.id.add_collector_proceed);
        proceed.setText(getResources().getString(R.string.addinv));
        vss=findViewById(R.id.vss_name);
        range=findViewById(R.id.range_name);
        division=findViewById(R.id.division_name);
        measurement=findViewById(R.id.measurement);
        checks.setValues(new TextInputEditText[]{vss,division,range});
        final Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = format1.format(d);
        date.setText(date1);
        quantity.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(4, 2)});

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        NtfpDao dao = SynchroniseDatabase.getInstance(this).ntfpDao();

        ArrayAdapter<CollectorsModel> collectorAdapter = new ArrayAdapter<CollectorsModel>(this,R.layout.multiline_spinner_dropdown_item,dao.getAllCollector());
        collector.setAdapter(collectorAdapter);

        ArrayAdapter<NTFP> ntfpAdapter = new ArrayAdapter<NTFP>(this,R.layout.multiline_spinner_dropdown_item,dao.getAllNTFPs());
        products.setAdapter(ntfpAdapter);

        ntfpTypeAdapter = new ArrayAdapter<>(this, R.layout.multiline_spinner_dropdown_item, dao.getAllNTFPTypes());
        ntfptype.setAdapter(ntfpTypeAdapter);
        member.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (collectorModel==null){
                    Toast.makeText(add_inventory.this, "Select collector first", Toast.LENGTH_SHORT).show();
                }else{
                    member.showDropDown();
                }
            }
        });
        member.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                if (collectorModel==null){
                    Toast.makeText(add_inventory.this, "Select collector first", Toast.LENGTH_SHORT).show();
                }else{
                    member.showDropDown();
                }

            }
        });



        SnackBarUtils.initAutoSpinner(new AutoCompleteTextView[]{products,collector});
        ntfptype.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (ntfpModel==null){
                    Toast.makeText(add_inventory.this, "Select NTFP first", Toast.LENGTH_SHORT).show();
                }else{
                    ntfptype.showDropDown();
                }
            }
        });
        ntfptype.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                if (ntfpModel==null){
                    Toast.makeText(add_inventory.this, "Select NTFP first", Toast.LENGTH_SHORT).show();
                }else{
                    ntfptype.showDropDown();
                }
            }
        });



        collector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                collectorModel = collectorAdapter.getItem(position);
                if (collectorModel!=null){
                    memberAdapter = new ArrayAdapter<MemberModel>(add_inventory.this,R.layout.multiline_spinner_dropdown_item,dao.getMemberFromMemberCId(collectorModel.getCid()));
                    member.setAdapter(memberAdapter);
                }
            }
        });

        member.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (collectorModel!=null && memberAdapter!=null){
                    memberModel = memberAdapter.getItem(position);
                }else
                    Toast.makeText(add_inventory.this, "select collector first", Toast.LENGTH_SHORT).show();
            }
        });

        products.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ntfpModel = ntfpAdapter.getItem(position);
                if (ntfpModel!=null){
                    ntfpTypeAdapter = new ArrayAdapter<NTFP.ItemType>(add_inventory.this,R.layout.multiline_spinner_dropdown_item,dao.getItemTypesFromNid(ntfpModel.getNid()));
                    ntfptype.setAdapter(ntfpTypeAdapter);
                }
            }
        });

        ntfptype.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ntfpModel!=null && ntfpTypeAdapter!=null){
                    itemTypeModel = ntfpTypeAdapter.getItem(position);
                }else
                    Toast.makeText(add_inventory.this, "select NTFP first", Toast.LENGTH_SHORT).show();
            }
        });

        quantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (itemTypeModel!=null && ntfpgrade.getSelectedItemPosition()>0)
                {
                    double basePrice;
                    if (ntfpgrade.getSelectedItemPosition()==1)
                        basePrice = itemTypeModel.getGrade1Price();
                    else if (ntfpgrade.getSelectedItemPosition() == 2)
                        basePrice = itemTypeModel.getGrade2Price();
                    else
                        basePrice = itemTypeModel.getGrade3Price();


                        String quntityM=quantity.getText().toString();
                    if (quntityM.length()>0) {


                        String amundBy=(String) measurement.getSelectedItem();
                        Log.i("getMEss",amundBy.toString()+"");
                        if(amundBy.equals("kilogram")){
                            amountPaid.setText(String.valueOf(Double.parseDouble(quntityM) * basePrice));
                        }else if (amundBy.equals("Gram")){
                        String amound = String.valueOf(Double.parseDouble(quntityM) * basePrice);
                           float fi=(Float.parseFloat(amound)/1000);

                         amountPaid.setText(String.valueOf(fi));
                        }



                    }else{
                        amountPaid.setText("");}
                }
                else{
                    Toast.makeText(add_inventory.this, "Select type and grade first", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    class DecimalDigitsInputFilter implements InputFilter {
        private Pattern mPattern;
        DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
            mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
        }
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Matcher matcher = mPattern.matcher(dest);
            if (!matcher.matches())
                return "";
            return null;
        }
    }

    public boolean checkSpinner(Spinner[] ets){
        boolean flag=true;
        for(Spinner et:ets){
            if (et.getSelectedItemPosition()==0){
                Toast.makeText(add_inventory.this,"Please "+et.getSelectedItem().toString() , Toast.LENGTH_SHORT).show();
                flag=false;
                break;
            }
        }
        return flag;
    }

    private class uploadTask extends AsyncTask<String,String,String> {

        private InventoryEntity model;
        private Model_payment payment;

        public uploadTask(InventoryEntity model,Model_payment payment){
            this.model = model;
            this.payment = payment;
        }
        private ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(add_inventory.this);
            dialog.setMessage("Submitting request");
            dialog.setCancelable(false);
            dialog.show();
            findViewById(R.id.spin_kit).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, strings[0]);
            Request request = new Request.Builder()
                    .url("http://13.127.166.242/NTFPAPI/API/VSSUppdateStatusForCollectorStock")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try {
                okhttp3.Response response = client.newCall(request).execute();
                if (setStatus(response.body().string()))
                    return getResources().getString(R.string.synced);
                else
                    return getResources().getString(R.string.somedetailsnotsynced);
            } catch (Exception e) {
                e.printStackTrace();
                return e.getClass().getSimpleName();
            }
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.dismiss();
            if (s.equals(getResources().getString(R.string.synced))){
                dao.insertInventory(model);
                db.insertData(payment);
                startActivity(new Intent(add_inventory.this, list_inventory.class));
            }
            else if(s.equals(getResources().getString(R.string.somedetailsnotsynced)))
                SnackBarUtils.WarningSnack(add_inventory.this,getString(R.string.somedetailsnotsynced));
            else if (s.equals("JSONException")||s.equals("SQLiteException"))
                SnackBarUtils.ErrorSnack(add_inventory.this,getString(R.string.somethingwentwrong));
            else
                SnackBarUtils.ErrorSnack(add_inventory.this,getString(R.string.servernotresponding));

        }
    }

    private boolean setStatus(String json) throws JSONException {
        Log.i("ksdks",json);
        boolean flag=true;
        JSONArray jsonArray=new JSONArray(json);
        for (int i=0;i<jsonArray.length();i++){
            JSONObject details=jsonArray.getJSONObject(i);
            if (details.getString("Status").equals("Success")) {
//                FCMNotifications.NotifyUser(add_inventory.this,"","","");
            }else{
                flag=false;
            }
        }
        return flag;
    }
    public void measurementSpinner(Spinner mSpinner, String compareValue) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.measurement, R.layout.spinner_layout);
        adapter.setDropDownViewResource(R.layout.spinner_layout);
        mSpinner.setAdapter(adapter);
        int arg2;
        if (compareValue != null) {
            arg2 = adapter.getPosition(compareValue);
            mSpinner.setSelection(arg2);
            if (arg2 != 0) {
                spinnerflag = true;
            }
        }
    }
    public void gradeSpinner(Spinner mSpinner, String compareValue) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.ntfp_grade, R.layout.spinner_layout);
        adapter.setDropDownViewResource(R.layout.spinner_layout);
        mSpinner.setAdapter(adapter);
        int arg2;
        if (compareValue != null) {
            arg2 = adapter.getPosition(compareValue);
            mSpinner.setSelection(arg2);
            if (arg2 != 0) {
                spinnerflag = true;
            }
        }
    }

}








