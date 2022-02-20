package nb.app.waterdelivery.adapters.admin_users;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

import nb.app.waterdelivery.R;
import nb.app.waterdelivery.adapters.EditJobChosenCustomerWatersAdapter;
import nb.app.waterdelivery.admin.users.AdminUpdateJobForUserActivity;
import nb.app.waterdelivery.alertdialog.EditJobOnDialogChoice;
import nb.app.waterdelivery.alertdialog.MyAlertDialog;
import nb.app.waterdelivery.alertdialog.OnDialogChoice;
import nb.app.waterdelivery.data.Customers;
import nb.app.waterdelivery.data.DatabaseHelper;
import nb.app.waterdelivery.data.JawDraft;
import nb.app.waterdelivery.data.SaveLocalDatas;
import nb.app.waterdelivery.data.Waters;
import nb.app.waterdelivery.jobs.EditMyJobActivity;


public class UpdateChosenCustomersListForJobAdapter extends RecyclerView.Adapter<UpdateChosenCustomersListForJobAdapter.ViewHolder> implements OnDialogChoice {

    private final String LOG_TITLE = "EditJobChosenCustomersAdapter";

    Context context;
    Activity activity;

    AdminUpdateJobForUserActivity emja;
    MyAlertDialog mad;
    LayoutInflater inflater;
    DatabaseHelper dh;
    SaveLocalDatas sld;

    ArrayList<Customers> customers_list;
    ArrayList<Waters> waters_list;
    ArrayList<JawDraft> draft_list;

    //Gyerek recycler
    UpdateChosenCustomersWaterListForJobAdapter adapter;
    int customer_cost;

    String[] waters_name_to_show;
    public boolean[] chosen_waters_boolean;
    boolean[] tmp_chosen_waters_boolean;

    int job_id;

    public UpdateChosenCustomersListForJobAdapter(Context context, Activity activity, ArrayList<Customers> customers_data, ArrayList<Waters> water_list, ArrayList<JawDraft> draft_list, int jobid) {
        this.inflater = LayoutInflater.from(context);
        this.customers_list = customers_data;
        this.waters_list = water_list;
        this.draft_list = draft_list;

        this.context = context;
        this.activity = activity;
        dh = new DatabaseHelper(context, activity);
        sld = new SaveLocalDatas(activity);
        this.emja = (AdminUpdateJobForUserActivity) context;
        this.mad = new MyAlertDialog(context, activity);

        this.customer_cost = 0;
        this.job_id = jobid;
    }

    @NonNull
    @Override
    public UpdateChosenCustomersListForJobAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.custom_chosen_customer_layout, parent, false);
        return new UpdateChosenCustomersListForJobAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateChosenCustomersListForJobAdapter.ViewHolder holder, int position) {
        ArrayList<JawDraft> child_draft_list = new ArrayList<>();

        //Elemek megadása
        holder.customer_name.setText(customers_list.get(position).getFullname().toUpperCase(Locale.ROOT));
        holder.customer_city.setText(customers_list.get(position).getCity());
        holder.customer_address.setText(customers_list.get(position).getAddress());

        if(customers_list.get(position).getBill() == 1) {
            holder.need_bill_icon.setVisibility(View.VISIBLE);
        } else {
            holder.need_bill_icon.setVisibility(View.GONE);
        }
        holder.need_bill_icon.setTooltipText("Kér számlát");

        //Megrendelőhöz tartozó összeg kiírása
        calculateCostForWater(holder, position);

        //Gyerek-adapter meghívás, adatok átadása
        dh.getJawDraftData("SELECT * FROM " + dh.EDITDRAFT + " WHERE CustomerID = " + customers_list.get(position).getId() + " AND JobID = " + job_id + ";", child_draft_list);//TODO Ide tenni a job ID-t

        //AlertDialog beállítások
        initializeArrays(waters_list.size());
        setShowableNames(waters_list);

        //
        holder.add_water.setOnClickListener(v -> {
            Log.i(LOG_TITLE, "Kattintás -> Víz hozzáadás (" + position + ")");
            setChosenWatersChecked(position);
            mad.AlertMultiSelectDialog("Válassz vizet", waters_name_to_show, chosen_waters_boolean, tmp_chosen_waters_boolean, "Rendben", "Mégse", holder, position,this);
        });

        holder.add_water.setOnLongClickListener(v -> {
            holder.add_water.setTooltipText("Vizek hozzáadása");
            return false;
        });


        loadElements(holder, child_draft_list);

    }

    private void loadElements(@NonNull UpdateChosenCustomersListForJobAdapter.ViewHolder holder, ArrayList<JawDraft> draft_list) {
        adapter = new UpdateChosenCustomersWaterListForJobAdapter(context, activity, waters_list, draft_list);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.recycler.setLayoutManager(manager);
        holder.recycler.setAdapter(adapter);
        Log.i(LOG_TITLE, "Megrendelők betöltése az AlertDialogba");
    }


    public void calculateCostForWater(@NonNull UpdateChosenCustomersListForJobAdapter.ViewHolder holder, int position) {
        customer_cost = 0;
        for(int i = 0; i < draft_list.size(); i++) {
            for(int j = 0; j < waters_list.size(); j++) {
                if (draft_list.get(i).getCustomerid() == customers_list.get(position).getId()) {
                    if (draft_list.get(i).getWaterid() == waters_list.get(j).getId()) {
                        customer_cost += draft_list.get(i).getWater_amount() * waters_list.get(j).getPrice();
                    }
                }
            }
        }
        @SuppressLint("DefaultLocale") String result_cost = String.format("%,d Ft", customer_cost).replace(",", " ");
        holder.cost.setText(result_cost);
    }


    @Override
    public int getItemCount() {
        return customers_list.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void OnPositiveClick(@NonNull RecyclerView.ViewHolder holder, int position) {
        int amount_of_chosen_waters = 0;
        for (boolean b : chosen_waters_boolean) {
            if (b) amount_of_chosen_waters++;
        }

        //Csak akkor fusson le, ha van kiválasztva víz
        if(amount_of_chosen_waters > 0) {
            Log.e(LOG_TITLE, "");

            //Először ürítjük a Draft táblát
            if (!dh.sql("DELETE FROM " + dh.EDITDRAFT + " WHERE CustomerID = " + customers_list.get(position).getId() + " AND JobID = " + job_id + ";")) return;//TODO átadni a job ID-t
            Log.i(LOG_TITLE, "Korábbi Draft elemek törölve.");

            //Töröljük a Customers And Waters tábla adatait is (A program feltételezi, hogy a választott vizek máskor is kellenek)
            if(!dh.sql("DELETE FROM " + dh.CAW + " WHERE CustomerID = " + customers_list.get(position).getId() + ";")) {
                Log.e(LOG_TITLE, "Nem sikerült törölni a CAW adatokat!");
                return;
            }

            //Feltöltjük az új adatokat
            for (int i = 0; i < chosen_waters_boolean.length; i++) {
                if (chosen_waters_boolean[i]) {
                    //Feltöltjük a Draft adatokat
                    if (!dh.sql("INSERT INTO " + dh.EDITDRAFT + " (JobID, CustomerID, WaterID) VALUES (" + job_id + ", " + customers_list.get(position).getId() + ", " + waters_list.get(i).getId() + ");")) {
                        Log.e(LOG_TITLE, "Nem sikerült feltölteni a vázlat adatokat. (DRAFT)");
                        return;
                    }
                    Log.i(LOG_TITLE, "::: A " + waters_list.get(i).getName() + " víz feltöltve. (" + waters_list.get(i).getId() + ")  [JAW-DRAFT]");

                    //Feltöltjük az új vizeket a felhasználókhoz
                    if (!dh.sql("INSERT INTO " + dh.CAW + " (CustomerID, WaterID) VALUES (" + customers_list.get(position).getId() + ", " + waters_list.get(i).getId() + ");")) {
                        Log.e(LOG_TITLE, "Nem sikerült feltölteni a megrendelő vizeit (CustomersAndWaters)");
                        return;
                    }
                    Log.i(LOG_TITLE, "::: A " + waters_list.get(i).getName() + " víz feltöltve. (" + waters_list.get(i).getId() + ") [CAW]");
                }
            }

            emja.all_caw_list.clear();
            dh.getCAWData(emja.all_caw_list, "");

            //Most a draftban átírjuk a mennyiségeket
            int draft_customer, draft_water;
            for(int i = 0; i < draft_list.size(); i++) {
                if(draft_list.get(i).getWater_amount() > 1 && draft_list.get(i).getCustomerid() == customers_list.get(position).getId() && draft_list.get(i).getJobid() == job_id) {
                    draft_customer = draft_list.get(i).getCustomerid();
                    draft_water = draft_list.get(i).getWaterid();

                    dh.sql("UPDATE " + dh.EDITDRAFT + " SET WaterAmount = " + draft_list.get(i).getWater_amount()
                            + " WHERE JobID =" + job_id + " AND CustomerID =" + draft_customer + " AND WaterID =" + draft_water);

                }
            }

            draft_list.clear();

            //Megjeleníteni az újakat
            emja.chosen_customers_list.clear();
            emja.loadDraftElements();
            emja.showCustomersInRecyclerView();
            emja.calculateGlobalIncome();

        } else {
            Toast.makeText(context, "Legalább egy víz legyen! Vagy töröld a megrendelő!", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TITLE, "Nem lett víz megadva.");
        }
    }

    @Override
    public void OnNegativeClick(@NonNull RecyclerView.ViewHolder holder, int position) {
        System.arraycopy(tmp_chosen_waters_boolean, 0, chosen_waters_boolean, 0, chosen_waters_boolean.length);
    }

    private void setChosenWatersChecked(int position) {
        Arrays.fill(chosen_waters_boolean, false);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < chosen_waters_boolean.length; i++) {
            for(int j = 0; j < draft_list.size(); j++) {
                if(draft_list.get(j).getWaterid() == waters_list.get(i).getId() && draft_list.get(j).getCustomerid() == customers_list.get(position).getId()) {
                    sb.append("true").append(",");
                    chosen_waters_boolean[i] = true;
                }
            }
        }

        Log.i(LOG_TITLE, "A szükséges elemek kipipálása AlertDialogban. (" + position + ". indexű elem)");
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView customer_name, customer_city, customer_address, cost;
        RecyclerView recycler;
        ImageView add_water, need_bill_icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            customer_name = itemView.findViewById(R.id.chosen_customer_name_gui);
            customer_city  = itemView.findViewById(R.id.chosen_customer_city_gui);
            customer_address = itemView.findViewById(R.id.chosen_customer_address_gui);
            cost = itemView.findViewById(R.id.chosen_customer_cost_gui);
            recycler = itemView.findViewById(R.id.chosen_customer_water_recycler);
            add_water = itemView.findViewById(R.id.add_more_water);
            need_bill_icon = itemView.findViewById(R.id.need_bill_icon);
        }
    }

    public void initializeArrays(int size) {
        waters_name_to_show = new String[size];
        chosen_waters_boolean = new boolean[size];
        tmp_chosen_waters_boolean = new boolean[size];
    }

    public void setShowableNames(ArrayList<Waters> list) {
        IntStream.range(0, list.size()).forEach(i -> waters_name_to_show[i] = list.get(i).getName());
    }
}
