package mil.nga.mapcache.view.map.feature;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mil.nga.geopackage.extension.schema.columns.DataColumns;
import mil.nga.geopackage.extension.schema.columns.DataColumnsDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.mapcache.GeoPackageMapFragment;
import mil.nga.mapcache.R;
import mil.nga.mapcache.viewmodel.GeoPackageViewModel;
import mil.nga.sf.GeometryType;

/**
 * Create a new view which holds a single point's data.  Opened after clicking a feature on the map
 */
public class PointView {

    /**
     * Context from where this was called
     */
    private Context context;

    /**
     * Geometry type
     */
    private GeometryType geometryType;

    /**
     * Feature row data
     */
    private FeatureRow featureRow;

    /**
     * DataColumnsDao
     */
    private DataColumnsDao dataColumnsDao;

    /**
     * GeoPackage name
     */
    private String geoName;

    /**
     * GeoPackage layer name
     */
    private String layerName;

    /**
     * ViewModel for accessing data from the repository
     */
    private GeoPackageViewModel geoPackageViewModel;

    public PointView(Context context, GeometryType geometryType, FeatureRow featureRow,
                     DataColumnsDao dataColumnsDao, String geoName, String layerName){
        this.context = context;
        this.geometryType = geometryType;
        this.featureRow = featureRow;
        this.dataColumnsDao = dataColumnsDao;
        this.geoName = geoName;
        this.layerName = layerName;
    }

    public void showPointData(){
        LayoutInflater inflater = LayoutInflater.from(context);
        View alertView = inflater.inflate(R.layout.feature_detail_popup, null);

        // Logo and title
        ImageView closeLogo = (ImageView) alertView.findViewById(R.id.feature_detail_close);
        closeLogo.setBackgroundResource(R.drawable.ic_clear_grey_800_24dp);
        TextView titleText = (TextView) alertView.findViewById(R.id.feature_detail_title);
        titleText.setText(geometryType.toString());

        // Open the dialog
        AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
                .setView(alertView);
        final AlertDialog alertDialog = dialog.create();

        // GeoPackage and layer
        TextView geoNameText = (TextView) alertView.findViewById(R.id.fc_geo_name);
        geoNameText.setText(geoName);
        TextView layerNameText = (TextView) alertView.findViewById(R.id.fc_layer_name);
        layerNameText.setText(layerName);


        // Feature Column recycler
        List<FcColumnDataObject> fcObjects = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        int geometryColumn = featureRow.getGeometryColumnIndex();
        for (int i = 0; i < featureRow.columnCount(); i++) {
            if (i != geometryColumn) {
                Object value = featureRow.getValue(i);
                if (value != null) {
                    String columnName = featureRow.getColumn(i).getName();
                    if (dataColumnsDao != null) {
                        try {
                            DataColumns dataColumn = dataColumnsDao.getDataColumn(featureRow.getTable().getTableName(), columnName);
                            if (dataColumn != null) {
                                columnName = dataColumn.getName();
                            }
                        } catch (SQLException e) {
                            Log.e(GeoPackageMapFragment.class.getSimpleName(),
                                    "Failed to search for Data Column name for column: " + columnName
                                            + ", Feature Table: " + featureRow.getTable().getTableName(), e);
                        }
                    }
                    FcColumnDataObject fcRow = new FcColumnDataObject(columnName, value);
                    fcObjects.add(fcRow);
                    message.append(columnName).append(": ");
                    message.append(value);
                    message.append("\n");
                }
            }
        }
        RecyclerView fcRecycler = alertView.findViewById(R.id.fc_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(alertView.getContext());
        fcRecycler.setLayoutManager(layoutManager);
        FeatureColumnAdapter fcAdapter = new FeatureColumnAdapter(fcObjects);
        fcRecycler.setAdapter(fcAdapter);


        // Click listener for close button
        closeLogo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }
}
