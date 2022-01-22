package nb.app.waterdelivery.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import nb.app.waterdelivery.R;
import nb.app.waterdelivery.admin.AllSettlementActivity;
import nb.app.waterdelivery.data.DatabaseHelper;
import nb.app.waterdelivery.data.SaveLocalDatas;
import nb.app.waterdelivery.data.Settlement;
import nb.app.waterdelivery.settlements.MySettlementsActivity;

public class AllSettlementsMonthAdapter extends RecyclerView.Adapter<AllSettlementsMonthAdapter.ViewHolder> {

    LayoutInflater inflater;
    Context context;
    Activity activity;
    ArrayList<String> months_list;
    DatabaseHelper dh;
    AllSettlementActivity msa;

    AllSettlementsAdapter adapter;
    ArrayList<Settlement> settlement_list;
    SaveLocalDatas sld;
    ArrayList<Boolean> expanded_list;

    public AllSettlementsMonthAdapter(Context context, Activity activity, ArrayList<String> m_list) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.activity = activity;
        this.months_list = m_list;
        dh = new DatabaseHelper(context, activity);
        msa = (AllSettlementActivity) context;

        sld = new SaveLocalDatas(activity);
        settlement_list = new ArrayList<>();
        this.expanded_list = new ArrayList<>();
        for(int i = 0; i < m_list.size(); i++) {
            expanded_list.add(false);
        }
    }

    @NonNull
    @Override
    public AllSettlementsMonthAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.custom_my_settlements_layout, parent, false);
        return new AllSettlementsMonthAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllSettlementsMonthAdapter.ViewHolder holder, int position) {
        holder.name.setText(months_list.get(position));
        String[] month = months_list.get(position).split("\\.");
        showElements(holder, month[0], getMonthsNumber(month[1]));

        boolean kinyitva = expanded_list.get(position);
        holder.item.setVisibility(kinyitva ? View.VISIBLE : View.GONE);

        holder.name.setOnClickListener(v -> {
            expanded_list.set(position, !kinyitva);
            notifyItemChanged(position);
        });
    }

    public void showElements(@NonNull AllSettlementsMonthAdapter.ViewHolder holder, String year, String month) {
        settlement_list.clear();
        dh.getSettlementData("SELECT * FROM " + dh.SETTLEMENT + " WHERE UserID = " + sld.loadUserID() + " AND YEAR(Created) = " + year + " AND MONTH(Created) = " + month + ";", settlement_list);
        adapter = new AllSettlementsAdapter(context,  activity, settlement_list);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.recycler.setLayoutManager(manager);
        holder.recycler.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return months_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        ConstraintLayout item;
        RecyclerView recycler;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.settlement_name_gui);
            item = itemView.findViewById(R.id.expand_layout);
            recycler = itemView.findViewById(R.id.settlements_for_month_recycler);
        }
    }

    public String getMonthsNumber(String num_of_month) {
        switch (num_of_month) {
            case "Január": return "1";
            case "Február": return "2";
            case "Március": return "3";
            case "Április": return "4";
            case "Május": return "5";
            case "Június": return "6";
            case "Július": return "7";
            case "Augusztus": return "8";
            case "Szeptember": return "9";
            case "Október": return "10";
            case "November": return "11";
            case "December": return "12";
        }
        return "0";
    }
}