package rw.sd.otobox;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mucyomiller.shoppingcart.model.Cart;
import com.mucyomiller.shoppingcart.model.Saleable;
import com.mucyomiller.shoppingcart.util.CartHelper;
import com.orhanobut.hawk.Hawk;
import com.parse.ParseException;
import com.parse.ParseObject;

import org.json.JSONArray;
import org.json.JSONException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import rw.sd.otobox.Models.CartItem;
import rw.sd.otobox.Models.Product;

public class OrderActivity extends AppCompatActivity {
    private static final String TAG = "OrderActivity";
    private Cart cart;
    private EditText names;
    private EditText phone;
    private EditText location;
    private EditText vin;
    private TextView totalprice,vat,totalitems;
    private Button send;
    private LottieAnimationView mSendAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        cart = (Cart) bundle.getParcelable("cart");
        totalprice = (TextView) findViewById(R.id.totalprice);
        vat        = (TextView) findViewById(R.id.vat);
        vin        = (EditText) findViewById(R.id.vin);
        totalitems = (TextView) findViewById(R.id.totalitems);
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        format.setCurrency(Currency.getInstance("RWF"));
        totalprice.setText("TOTAL : "+format.format(cart.getTotalPrice()));
        vat.setText("VAT : "+(cart.getTotalPrice().multiply(BigDecimal.valueOf(18))).divide(BigDecimal.valueOf(100))+" RWF");
        totalitems.setText("Items : "+cart.getTotalQuantity());

        Log.d(TAG, "onCreate: ");
        names = (EditText) findViewById(R.id.names);
        phone = (EditText) findViewById(R.id.phone);
        location = (EditText) findViewById(R.id.location);
        mSendAnimation = findViewById(R.id.send_loading_animation);
        send = (Button) findViewById(R.id.send);

        //check if userInfo exists and pre-fill user user opted-in
        if(Hawk.contains("userInfo")){
            new MaterialDialog.Builder(this)
                    .title("pre-fill data")
                    .content("It's looks likes you have used this form before\n" +
                            "do you prefer to use previous form data?")
                    .positiveText("Yes")
                    .positiveColor(getResources().getColor(R.color.colorPrimary))
                    .negativeText("No")
                    .negativeColor(getResources().getColor(R.color.red))
                    .onPositive((dialog, which) -> {
                        ArrayList<String> userInfo = Hawk.get("userInfo");
                        names.setText(userInfo.get(0));
                        phone.setText(userInfo.get(1));
                        location.setText(userInfo.get(2));
                        vin.setText(userInfo.get(3));

                    }).onNegative((dialog, which) -> {
                        Hawk.delete("userInfo");
                    })
                    .show();
        }

        send.setOnClickListener(v->{
            if(validate())
            {
                //disable button
                send.setVisibility(View.GONE);
                mSendAnimation.setVisibility(View.VISIBLE);
                //getting cart items
                List<CartItem> cartItems = new ArrayList<CartItem>();
                Map<Saleable, Integer> itemMap = (Map<Saleable, Integer>)cart.getItemWithQuantity();
                for (Map.Entry<Saleable, Integer> entry : itemMap.entrySet()) {
                    CartItem cartItem = new CartItem();
                    cartItem.setProduct((Product)entry.getKey());
                    cartItem.setQuantity(entry.getValue());
                    cartItems.add(cartItem);
                }
                Gson gson = new Gson();
                String listString = gson.toJson(cartItems, new TypeToken<ArrayList<CartItem>>() {}.getType());
                try {
                    JSONArray itemsArray =  new JSONArray(listString);
                    Log.d(TAG, "onCreate: itemsArray"+itemsArray.toString());
                    ParseObject orderObject = new ParseObject("Order");
                    orderObject.put("names", names.getText().toString());
                    orderObject.put("phone",phone.getText().toString());
                    orderObject.put("location",location.getText().toString());
                    orderObject.put("items",itemsArray);
                    orderObject.put("amount", cart.getTotalPrice());
                    orderObject.put("vat", (cart.getTotalPrice().multiply(BigDecimal.valueOf(18))).divide(BigDecimal.valueOf(100)));
                    orderObject.put("itemcount",cart.getTotalQuantity());
                    orderObject.put("vin",vin.getText().toString());
                    orderObject.saveInBackground(e -> {
                        if(e== null){
                            //saving user infos on disk
                            ArrayList<String> userInfo = new ArrayList<>();
                            userInfo.add(names.getText().toString());
                            userInfo.add(phone.getText().toString());
                            userInfo.add(location.getText().toString());
                            userInfo.add(vin.getText().toString());
                            //commit to disk
                            Hawk.put("userInfo",userInfo);
                            Toasty.info(getApplicationContext(),"Order Sent Successfull",Toast.LENGTH_LONG).show();
                            //clearing cart after sending order!
                            CartHelper.getCart().clear();
                            // redirect back to MainActivity
                            Intent mIntent = new Intent(getApplicationContext(),MainActivity.class);
                            mIntent.putExtra("goto_new_arrivals",true);
                            finish();
                            startActivity(mIntent);
                        }else
                        {
                            Toasty.error(getApplicationContext(),"Error occured in saving try again!",Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            send.setVisibility(View.VISIBLE);
                            mSendAnimation.setVisibility(View.GONE);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    send.setVisibility(View.VISIBLE);
                    mSendAnimation.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean validate() {
        if(names.getText().toString().isEmpty()){
              names.setError("Amazina Arakenewe!");
              return false;
        }
        if(phone.getText().toString().isEmpty()){
            names.setError("Telephoni Arakenewe!");
            return false;
        }
        if(location.getText().toString().isEmpty()){
            names.setError("Ahubarizwa Hakenewe!");
            return false;
        }
        if(vin.getText().toString().isEmpty()){
            names.setError("Nimero iranga ikinyabiziga irakenewe!");
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
