package fr.npeloton.npforkaroo.ui.karoo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import fr.npeloton.npforkaroo.R;
import fr.npeloton.npforkaroo.ui.GpsiesItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class KarooFragment extends Fragment {

    Context mContext;


    String searchString="";
    private RecyclerView recyclerView;
    SwipeRefreshLayout sw_refresh;
    SearchView searchView=null;
    String FLUX_KAROO= "https://dashboard.hammerhead.io/v1/users/11603/routes?per_page=100";

    ArrayList<GpsiesItem> gpsiesItems;
    View homeView = null;
    ViewGroup mViewGroup;


    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if(gpsiesItems==null){
            gpsiesItems = new ArrayList<>();
            Log.d("onViewCreated", "gpsiesItems reset");

        }
        if(sw_refresh==null){
            sw_refresh = view.findViewById(R.id.sw_refresh_karoo);
        }





        haveStoragePermission();


        fillRecycleView();




        sw_refresh.setOnRefreshListener(new  SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(){
                //gpsiesItems.clear();
                new GetXMLdata().execute("");
                fillRecycleView();
                sw_refresh.setRefreshing(false);
            }
        });
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //  View view = inflater.inflate(R.layout.fragment_home, container, false);
        homeView = inflater.inflate(R.layout.fragment_karoo_garmin, container, false);
        mContext = getContext();
        KarooAPI.fullProcessToken(getContext());
        new GetXMLdata().execute("");

        return homeView;
    }





    private  void fillRecycleView(){



        //recycler
        recyclerView = homeView.findViewById(R.id.liste_karoo);


        //définit l'agencement des cellules, ici de façon verticale, comme une ListView
        if((recyclerView!=null)&&(gpsiesItems!=null)) {

            recyclerView.setLayoutManager(new LinearLayoutManager(homeView.getContext()));

            //pour adapter en grille comme une RecyclerView, avec 2 cellules par ligne
            //recyclerView.setLayoutManager(new GridLayoutManager(this,2));

            //puis créer un MyAdapter, lui fournir notre liste de villes.
            //cet adapter servira à remplir notre recyclerview
            Log.d("fillRecycleView", "gpsiesItems " + gpsiesItems.size());

            recyclerView.setAdapter(new MyAdapter(gpsiesItems));


        } else {
            Log.e("ERROR", "fillRecycleView: recyclerView empty");
        }

    }


    private void readFluxJson(){


        KarooAPI.printdebug("readFluxJson", "");
        //RequestBody requestBody = new FormBody.Builder()
        //      .build();

        KarooAPI. printdebug("Authorisation ","Bearer " + KarooAPI.getToken(mContext));
        Request request = new Request.Builder().
                header("Authorization", "Bearer " + KarooAPI.getToken(mContext))
                .url(FLUX_KAROO)
                .get()
                .build();



        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();
                try {
                    JSONObject res = new JSONObject(mMessage);
                    int num = (int) res.get("totalItems");
                    KarooAPI.printdebug("totalItems", num + "" );
                    KarooAPI.printdebug("gpsiesItems.size()", gpsiesItems.size() + "" );
                    if(gpsiesItems.size()<num) {
                        //JSONObject data = (JSONObject) res.get("data");
                        JSONArray array = res.getJSONArray("data");
                        for (int i = 0; i < array.length(); i++) {
                            GpsiesItem currentItem = new GpsiesItem();

                            currentItem.title = array.getJSONObject(i).getString("name");
                            currentItem.km = String.valueOf(array.getJSONObject(i).getLong("distance") / 1000);

                          //String date =   array.getJSONObject(i).getString("createdAt");
                            currentItem.summuryPolyline = array.getJSONObject(i).getString("summaryPolyline");
                            gpsiesItems.add(currentItem);
                            KarooAPI.printdebug("gpsiesItems title", currentItem.title);
                        }
                        KarooAPI.toastInResponse(mContext, "num " + num);
                    }
                } catch (JSONException e) {
                    Log.w("readFluxJson JSon resp", mMessage);
                    //KarooAPI.toastInResponse(mContext, "Ride not uploaded "+mMessage);

                }


                Log.w("readFluxJson", mMessage);

            }

        });





    }


    public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        ArrayList<GpsiesItem> list;

        //ajouter un constructeur prenant en entrée une liste
        public MyAdapter(ArrayList<GpsiesItem> list) {
            this.list = list;
        }

        //cette fonction permet de créer les viewHolder
        //et par la même indiquer la vue à inflater (à partir des layout xml)
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {

            //  LayoutInflater li = (LayoutInflater) getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //View view = li.inflate(R.layout.mapcard,viewGroup,false);

//           View view = LayoutInflater.from(mViewGroup.getContext()).inflate(R.layout.mapcard,viewGroup,false);
            View view =  LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mapcard,viewGroup,false);
            return new MyViewHolder(view);
        }

        //c'est ici que nous allons remplir notre cellule avec le texte/image de chaque MyObjects
        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, int position) {
            GpsiesItem myObject = list.get(position);
            if(myObject!=null) {
                myViewHolder.bind(myObject);
            } else {
                Log.d("HomeFrgt:onBindViewHldr","myObject==null");
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }
    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewView;
        private TextView kmView;

        private ImageView imageView;
        private Button buttonKarooUpload;
        //     private ImageButton buttonOpen;
        //   private ImageButton buttonOpenOsm;

        //itemView est la vue correspondante à 1 cellule
        public MyViewHolder(View itemView) {
            super(itemView);

            //c'est ici que l'on fait nos findView

            textViewView = (TextView) itemView.findViewById(R.id.text);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            buttonKarooUpload = (Button)itemView.findViewById(R.id.karoo_upload);
              kmView =  (TextView) itemView.findViewById(R.id.km);
            buttonKarooUpload.setVisibility(View.INVISIBLE);
        }

        //puis ajouter une fonction pour remplir la cellule en fonction d'un MyObject
        public void bind(final GpsiesItem myItem) {
            if(myItem!=null){
                textViewView.setText(myItem.getTitle());
                 kmView.setText(myItem.getKm());

            }


            textViewView.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //openGPX(myItem);
                }

            });



            if(myItem !=null && imageView!=null) {
                Picasso.get().load("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Openstreetmap_logo.svg/1200px-Openstreetmap_logo.svg.png").centerCrop().fit().into(imageView);


            }
        }


    }
    public  boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission debug","You have permission");
                return true;
            } else {

                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error","You already have the permission");
            return true;
        }
    }







    private class GetXMLdata extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            Log.d("doInBackground", "readfluxjson");
            readFluxJson();
            fillRecycleView();
            return "Executed";
        }


        @Override
        protected void onPostExecute(String s) {
           // super.onPostExecute(s);
            //    readGpsiesItem();
            Log.d("onPostExecute", "fillRecycleView");

            fillRecycleView();

        }

    }



}