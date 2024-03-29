package nb.app.waterdelivery.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import nb.app.waterdelivery.R;
import nb.app.waterdelivery.alertdialog.MyAlertDialog;
import nb.app.waterdelivery.data.DatabaseHelper;
import nb.app.waterdelivery.data.Jobs;
import nb.app.waterdelivery.data.Settlement;
import nb.app.waterdelivery.jobs.MyJobsActivity;
import nb.app.waterdelivery.settlements.MySettlementsActivity;

public class MySettlementsChildAdapter extends RecyclerView.Adapter<MySettlementsChildAdapter.ViewHolder> {

    private final String LOG_TITLE = "MySettlementsAdapter";

    LayoutInflater inflater;
    Context context;
    Activity activity;
    ArrayList<Settlement> settlement_list;
    DatabaseHelper dh;
    MyAlertDialog mad;
    MySettlementsActivity mja;

    public MySettlementsChildAdapter(Context context, Activity activity, ArrayList<Settlement> settlement_list) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.activity = activity;
        this.settlement_list = settlement_list;
        dh = new DatabaseHelper(context, activity);
        mad = new MyAlertDialog(context, activity);
        mja = (MySettlementsActivity) context;
    }

    @NonNull
    @Override
    public MySettlementsChildAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.custom_settlmentname_layout, parent, false);
        return new MySettlementsChildAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MySettlementsChildAdapter.ViewHolder holder, int position) {
        Settlement settlement = settlement_list.get(position);
        holder.name.setText(settlement.getName());

        holder.item.setOnClickListener(v -> {
            StringBuilder dialog_message = new StringBuilder();

            dialog_message.append("Munkák száma: ").append(dh.getExactInt("SELECT COUNT(*) FROM " + dh.JIS + " WHERE settlementID = " + settlement.getId())).append("\n");
            dialog_message.append("Megrendelők száma: ").append(dh.getExactInt("SELECT COUNT(*) FROM " + dh.CIJ + " cij, " + dh.JIS + " jis WHERE cij.JobID = jis.JobID AND settlementid = " + settlement.getId())).append("\n").append("\n");
            dialog_message.append("Leadott vizek száma:").append("\n");
            dialog_message.append(getWaters("SELECT w.Name, SUM(WaterAmount) FROM " + dh.JIS + " jis, " + dh.JAW + " jaw, " + dh.WATERS + " w WHERE jaw.JobID = jis.JobID AND w.ID = jaw.WaterID AND settlementid = " + settlement.getId() + " GROUP BY w.Name")).append("\n");
            dialog_message.append("Teljes bevétel: ").append(dh.getExactInt("SELECT SUM(w.Price * jaw.WaterAmount) As Ár FROM " + dh.JIS + " jis, " + dh.JAW + " jaw, " + dh.WATERS + " w WHERE jaw.JobID = jis.JobID AND w.ID = jaw.WaterID AND settlementid = " + settlement.getId())).append(" Ft").append("\n").append("\n");

            if(dh.getExactInt("SELECT finisherid FROM " + dh.SETTLEMENT + " WHERE ID = " + settlement.getId() + ";") != 0) {
                dialog_message.append(dh.getExactString("SELECT u.Fullname FROM " + dh.SETTLEMENT + " s, " + dh.USERS + " u WHERE s.finisherID = u.ID AND s.ID = " + settlement.getId() + ";")).append(" jóváhagyta!");

                String date = dh.getExactString("SELECT Finished FROM " + dh.SETTLEMENT + " WHERE ID = " + settlement.getId() + ";");
                String[] date_array = date.split("\\.");
                dialog_message.append(date_array[0]);
            } else {
                dialog_message.append("Még nincs jóváhagyva!");
            }

            mad.AlertInfoDialog("Munka adatok",dialog_message.toString(), "Rendben");
        });

    }

    public String getWaters(String select) {
        Connection con = null;
        try {
            con = dh.connectionClass(context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder result = new StringBuilder();

        try {
            if(con != null) {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(select);

                while(rs.next()) {
                    result.append(rs.getString(1)).append(": ").append(rs.getString(2)).append(" db").append("\n");
                }
            }
            Log.i(LOG_TITLE, "SIKERES (" + select + ")");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            Log.e(LOG_TITLE, "SIKERTELEN (" + select + ")");
        }

        return result.toString();
    }

    @Override
    public int getItemCount() {
        return settlement_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        CardView item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.custom_user_name_gui);
            item = itemView.findViewById(R.id.custom_user_item);
        }
    }
}
