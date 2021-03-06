package rw.sd.otobox;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.juanlabrador.badgecounter.BadgeCounter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.mucyomiller.shoppingcart.model.Cart;
import com.mucyomiller.shoppingcart.util.CartHelper;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import rw.sd.otobox.Adapters.DescriptionAdapter;
import rw.sd.otobox.Adapters.ModelsAdapter;
import rw.sd.otobox.Event.CartEvent;
import rw.sd.otobox.Models.Description;
import rw.sd.otobox.Models.Product;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private Product mProduct;
    private ImageView product_logo;
    private Cart cart;
    private TextView product_name,model_name,generation_name,generation_released,product_price,product_condition;
    RecyclerView prod_details;
    private Button product_detail_add_to_cart;
    private RatingBar product_quality;
    public List<Description> descriptionList;
    public DescriptionAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cart =  CartHelper.getCart();
        product_logo = (ImageView) findViewById(R.id.product_logo);
        product_name = (TextView) findViewById(R.id.product_name);
        model_name   = (TextView) findViewById(R.id.model_name);
        generation_name = (TextView) findViewById(R.id.generation_name);
        generation_released = (TextView) findViewById(R.id.generation_released);
        product_price      = (TextView) findViewById(R.id.product_price);
        product_quality = (RatingBar) findViewById(R.id.product_quality);
        product_condition = (TextView) findViewById(R.id.product_condition);
        //recyclerview item & configs
        prod_details = (RecyclerView) findViewById(R.id.prod_details);
        descriptionList = new ArrayList<>();
        adapter = new DescriptionAdapter(this, descriptionList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        prod_details.setLayoutManager(mLayoutManager);
        prod_details.setAdapter(adapter);
        //add to cart button
        product_detail_add_to_cart = (Button) findViewById(R.id.product_detail_add_to_cart);
        //getting intents
        mProduct = getIntent().getParcelableExtra("product");
        try{
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL);
            requestOptions.fallback(R.drawable.otobox_start);
            requestOptions.error(R.drawable.otobox_start);
            requestOptions.placeholder(R.drawable.otobox_start);
            Glide.with(this)
                    .load(mProduct.getThumbnail())
                    .apply(requestOptions)
                    .into(product_logo);
            product_name.setText(mProduct.getName());
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            format.setCurrency(Currency.getInstance("RWF"));
            format.setMinimumFractionDigits(0);
            product_price.setText(format.format(mProduct.getPrice()));
            product_quality.setRating(mProduct.getWarranty());
            product_quality.setNumStars(5);
            product_condition.setText(mProduct.getQuality());
            //getting unavailable datas
//            Toasty.error(getApplicationContext(),"first =>"+mProduct.getpId(), Toast.LENGTH_SHORT).show();
            ParseQuery<ParseObject> mParse = ParseQuery.getQuery("Spare");
            mParse.whereEqualTo("objectId", mProduct.getpId());
            mParse.include("generation");
            mParse.include("generation.model");
            mParse.getFirstInBackground((object, e) -> {
                if(e == null){
                    if (object != null) {
                        if(object.getParseObject("generation") != null){
                            model_name.setText(object.getParseObject("generation").getParseObject("model").get("name").toString());
                            generation_name.setText(object.getParseObject("generation").get("name").toString());
                            generation_released.setText(object.getParseObject("generation").get("released").toString());
                        }else
                        {
                        Toasty.info(getApplicationContext(),"No Generation Found!",Toast.LENGTH_SHORT,true).show();
                        }

                        //Populate ListViewCompat
                        String descr = object.get("description") != null ? object.get("description").toString() : null;
                       if(!descr.isEmpty()){
                           try {
                               JSONArray dataArr = new JSONArray(descr);
//                            Log.d(TAG,dataArr.toString());
                               for (int i = 0;i<dataArr.length();i++) {
                                   JSONObject jsonObject = dataArr.getJSONObject(i);
                                   Description mDescription = new Description();
                                   mDescription.setDesc_name(jsonObject.getString("name")+" :");
                                   mDescription.setDesc_desc(jsonObject.getString("desc"));
                                   descriptionList.add(mDescription);
                               }
                               adapter.notifyDataSetChanged();

                           } catch (JSONException e1) {
                               Log.e(TAG, e1.getMessage());
                               e1.printStackTrace();
                           }
                       }

                    } else {
                        Toasty.info(getApplicationContext(),"No Description Found! ", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toasty.error(getApplicationContext(),"Error Occurred!"+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            product_detail_add_to_cart.setOnClickListener(v->{
                if(mProduct != null){
                    Log.d(TAG, "Adding product: " + mProduct.getName());
                    cart.add(mProduct,1);
                    //broadcast Cart change Event!
                    CartEvent mCartEvent = new CartEvent("ADD",cart);
                    ((App)v.getContext().getApplicationContext()).bus().send(mCartEvent);
                    Toasty.success(v.getContext(), mProduct.getName()+" added to Cart!", Toast.LENGTH_SHORT).show();
                }else
                {
                    Toasty.error(v.getContext(), " Error Occured!", Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e)
        {
            Toasty.error(getApplicationContext(),"Error Occurred! "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_buy, menu);
        Cart cart = CartHelper.getCart();

        //showing previous cart datas if available
        int mNotificationCounter = cart.getProducts().size();
        // Create a condition (hide it if the count == 0)
        if (mNotificationCounter > 0) {
            BadgeCounter.update(this,
                    menu.findItem(R.id.action_cart),
                    R.drawable.icon_cart,
                    BadgeCounter.BadgeColor.RED,
                    mNotificationCounter);
        } else {
            BadgeCounter.hide(menu.findItem(R.id.action_cart));
        }

        ((App)getApplication()).bus().toObservable().subscribe(event -> {
            if(event instanceof CartEvent){
                Log.d(TAG, " RxBus Cart Change event detected ! =>"+ ((CartEvent) event).getAction());
                // Create a condition (hide it if the count == 0)
                if (cart.getProducts().size() > 0) {
                    BadgeCounter.update(this,
                            menu.findItem(R.id.action_cart),
                            R.drawable.icon_cart,
                            BadgeCounter.BadgeColor.RED,
                            cart.getProducts().size());
                } else {
                    BadgeCounter.hide(menu.findItem(R.id.action_cart));
                }
            }
        });
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
            case R.id.action_search:
                return true;
            case R.id.action_about:
                Intent mSearchIntent = new Intent(ProductDetailActivity.this,AboutActivity.class);
                startActivity(mSearchIntent);
                return true;
            case R.id.action_cart:
//                Toast.makeText(this, "Clicked cart", Toast.LENGTH_SHORT).show();
                Intent mCartIntent = new Intent(getApplicationContext(),CartActivity.class);
                startActivity(mCartIntent);
                return true;
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
