package fr.npeloton.npforkaroo.ui.home;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
import fr.npeloton.npforkaroo.ui.karoo.KarooAPI;

public class HomeFragment extends Fragment {

    Context mContext;
    String searchString="";
    private RecyclerView recyclerView;
    SwipeRefreshLayout sw_refresh;
    SearchView searchView=null;
    String FLUX_CSV ="https://n-peloton.fr/getMapCsv.php";
    ArrayList<GpsiesItem> gpsiesItems;
    View homeView = null;
    ViewGroup mViewGroup;



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if(gpsiesItems==null){
            gpsiesItems = new ArrayList<>();
        }
        if(sw_refresh==null){
            sw_refresh = view.findViewById(R.id.sw_refresh);
        }


        haveStoragePermission();


        new GetCSVdata().execute(FLUX_CSV);
        fillRecycleView();




        sw_refresh.setOnRefreshListener(new  SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(){
                searchString="";
                //   searchView.setQuery("",true);
                gpsiesItems.clear();
                new GetCSVdata().execute(FLUX_CSV);
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
        homeView = inflater.inflate(R.layout.fragment_home, container, false);
        mContext = getContext();

        return homeView;
    }

    private void readFluxCSV(String FLUX_CSV){


        GpsiesItem currentItem = null;
        try{
            InputStream is = new URL(FLUX_CSV).openStream();
            InputStreamReader isr = new InputStreamReader(is);

            CSVReader reader = new CSVReader(isr);
            String [] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String[] parts = nextLine[0].split(";");
                if(parts.length>0){

                    String title = parts[1];
                    String km = parts[2];
                    String elevation = parts[3];
                    if(title.toUpperCase().contains(searchString.toUpperCase() )){
                        currentItem = new GpsiesItem(parts[0],parts[1],km, elevation);
                        gpsiesItems.add(currentItem);
                    }




                }
            }
        }catch(Exception e){
            e.printStackTrace();
            Log.e(e.getClass().toString(), e.getMessage());
        }


    }



    private  void fillRecycleView(){
        View tempview = getLayoutInflater().inflate(R.layout.fragment_home, mViewGroup, false);



        //recycler
        recyclerView = homeView.findViewById(R.id.liste);


        //définit l'agencement des cellules, ici de façon verticale, comme une ListView
        if(recyclerView!=null) {

            recyclerView.setLayoutManager(new LinearLayoutManager(homeView.getContext()));

            //pour adapter en grille comme une RecyclerView, avec 2 cellules par ligne
            //recyclerView.setLayoutManager(new GridLayoutManager(this,2));

            //puis créer un MyAdapter, lui fournir notre liste de villes.
            //cet adapter servira à remplir notre recyclerview

            recyclerView.setAdapter(new MyAdapter(gpsiesItems));


        } else {
            Log.e("ERROR", "fillRecycleView: recyclerView empty");
        }

    }


    private class GetCSVdata extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            readFluxCSV(strings[0]);
            Log.d("readFluxCSV",strings[0]);
            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            fillRecycleView();
        }
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

        private ImageButton buttonDl;
        private ImageButton buttonOpen;
        private ImageButton buttonOpenOsm;

        //itemView est la vue correspondante à 1 cellule
        public MyViewHolder(View itemView) {
            super(itemView);

            //c'est ici que l'on fait nos findView

            textViewView = (TextView) itemView.findViewById(R.id.text);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            kmView =  (TextView) itemView.findViewById(R.id.km);
            buttonKarooUpload = (Button)itemView.findViewById(R.id.karoo_upload);

        }

        //puis ajouter une fonction pour remplir la cellule en fonction d'un MyObject
        public void bind(final GpsiesItem myItem) {
            if(myItem!=null){
                textViewView.setText(myItem.getTitle());
               kmView.setText(myItem.getDescription());

            }


            textViewView.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //openGPX(myItem);
                }

            });

            buttonKarooUpload.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //    openGPX(myItem);
                    if(KarooAPI.getToken(getContext())!=null) {
                        if(KarooAPI.isTokenExpired(mContext)){
                            KarooAPI.refreshToken(mContext);

                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //creating new thread to handle Http Operations
                                KarooAPI.pushUrl(getContext(),myItem.getDirectLinkNPGpx());
                            }
                        }).start();

                    } else {
                        Log.d("No token","test");
                        new GetTokenBeforePosting().execute("");
                    }

                }

            });

       /**      imageView.setOnClickListener(new AdapterView.OnClickListener(){
                @Override
                public void onClick(View view) {
                   // openVueAllTrails(myItem);
                }
            });

            buttonDl.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                //    openGPX(myItem);
                }

            });

            buttonOpen.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                  //  openMapsOnGpsies(myItem);
                }

            });
            buttonOpenOsm.setOnClickListener(new AdapterView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //openGPXOsmand(myItem);

                }

            });
**/


            if(myItem !=null && imageView!=null) {
                Picasso.get().load(myItem.getImageUrl()).centerCrop().fit().into(imageView);
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

    private class GetTokenBeforePosting extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            if(KarooAPI.getToken(mContext)==null){
                Log.d("doinbachgroun"," getToken null");

                KarooAPI.requestToken(mContext);
            } else if (KarooAPI.isTokenExpired(mContext)){
                Log.d("doinbachgroun"," getToken not null");

                KarooAPI.refreshToken(mContext);
            }


            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {


            String access_token =  KarooAPI.getToken(mContext);
            if(access_token!=null) {


                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        //creating new thread to handle Http Operations
                        // uploadFile();

                    }

                }).start();

            } else {
                Log.d("No token","test");
                Toast.makeText(mContext, "No token, you must login once",Toast.LENGTH_SHORT).show();


            }

            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}