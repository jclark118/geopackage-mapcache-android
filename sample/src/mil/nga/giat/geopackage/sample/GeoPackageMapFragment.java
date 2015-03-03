package mil.nga.giat.geopackage.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.geopackage.BoundingBox;
import mil.nga.giat.geopackage.GeoPackage;
import mil.nga.giat.geopackage.GeoPackageException;
import mil.nga.giat.geopackage.GeoPackageManager;
import mil.nga.giat.geopackage.factory.GeoPackageFactory;
import mil.nga.giat.geopackage.features.user.FeatureCursor;
import mil.nga.giat.geopackage.features.user.FeatureDao;
import mil.nga.giat.geopackage.features.user.FeatureRow;
import mil.nga.giat.geopackage.geom.Geometry;
import mil.nga.giat.geopackage.geom.GeometryType;
import mil.nga.giat.geopackage.geom.LineString;
import mil.nga.giat.geopackage.geom.conversion.GoogleMapShapeConverter;
import mil.nga.giat.geopackage.geom.data.GeoPackageGeometryData;
import mil.nga.giat.geopackage.tiles.overlay.GoogleAPIGeoPackageOverlay;
import mil.nga.giat.geopackage.tiles.user.TileDao;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;

/**
 * Map Fragment for showing GeoPackage features and tiles
 * 
 * @author osbornb
 */
public class GeoPackageMapFragment extends Fragment implements
		OnMapLongClickListener, OnMapClickListener, OnMarkerClickListener,
		OnMarkerDragListener, ILoadTilesTask {

	/**
	 * Max features key for saving to preferences
	 */
	private static final String MAX_FEATURES_KEY = "max_features_key";

	/**
	 * Map type key for saving to preferences
	 */
	private static final String MAP_TYPE_KEY = "map_type_key";

	/**
	 * Get a new instance of the fragment
	 * 
	 * @return
	 */
	public static GeoPackageMapFragment newInstance() {
		GeoPackageMapFragment mapFragment = new GeoPackageMapFragment();
		return mapFragment;
	}

	/**
	 * Active GeoPackages
	 */
	private GeoPackageDatabases active;

	/**
	 * Google map
	 */
	private GoogleMap map;

	/**
	 * View
	 */
	private static View view;

	/**
	 * Load tiles view
	 */
	private static View loadTilesView;

	/**
	 * Edit features view
	 */
	private static View editFeaturesView;

	/**
	 * GeoPackage manager
	 */
	private GeoPackageManager manager;

	/**
	 * Update task
	 */
	private MapUpdateTask updateTask;

	/**
	 * Mapping of open GeoPackages by name
	 */
	private Map<String, GeoPackage> geoPackages = new HashMap<String, GeoPackage>();

	/**
	 * Vibrator
	 */
	private Vibrator vibrator;

	/**
	 * Touchable map layout
	 */
	private TouchableMap touch;

	/**
	 * Bounding box mode
	 */
	private boolean boundingBoxMode = false;

	/**
	 * Edit features mode
	 */
	private boolean editFeaturesMode = false;

	/**
	 * Bounding box starting corner
	 */
	private LatLng boundingBoxStartCorner = null;

	/**
	 * Bounding box ending corner
	 */
	private LatLng boundingBoxEndCorner = null;

	/**
	 * Bounding box polygon
	 */
	private Polygon boundingBox = null;

	/**
	 * True when drawing a shape
	 */
	private boolean drawing = false;

	/**
	 * Bounding Box menu item
	 */
	private MenuItem boundingBoxMenuItem;

	/**
	 * Bounding box clear button
	 */
	private ImageButton boundingBoxClearButton;

	/**
	 * Edit Features menu item
	 */
	private MenuItem editFeaturesMenuItem;

	/**
	 * Edit features database
	 */
	private String editFeaturesDatabase;

	/**
	 * Edit features table
	 */
	private String editFeaturesTable;

	/**
	 * Mapping between marker ids and the feature ids
	 */
	private Map<String, Long> editFeatureIds = new HashMap<String, Long>();

	/**
	 * Edit points type
	 */
	private EditType editFeatureType = null;

	/**
	 * Edit type enumeration
	 */
	private enum EditType {

		POINT, LINESTRING, POLYGON;

	}

	/**
	 * Map of edit point marker ids and markers
	 */
	private Map<String, Marker> editPoints = new LinkedHashMap<String, Marker>();

	/**
	 * Edit linestring
	 */
	private Polyline editLinestring;

	/**
	 * Edit polygon
	 */
	private Polygon editPolygon;

	/**
	 * Edit point button
	 */
	private ImageButton editPointButton;

	/**
	 * Edit linestring button
	 */
	private ImageButton editLinestringButton;

	/**
	 * Edit polygon button
	 */
	private ImageButton editPolygonButton;

	/**
	 * Edit accept button
	 */
	private ImageButton editAcceptButton;

	/**
	 * Edit clear button
	 */
	private ImageButton editClearButton;

	/**
	 * Constructor
	 */
	public GeoPackageMapFragment() {

	}

	/**
	 * Set the active databases
	 * 
	 * @param active
	 */
	public void setActive(GeoPackageDatabases active) {
		this.active = active;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		vibrator = (Vibrator) getActivity().getSystemService(
				Context.VIBRATOR_SERVICE);

		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null) {
				parent.removeView(view);
			}
		}

		try {
			view = inflater.inflate(R.layout.fragment_map, container, false);
		} catch (InflateException e) {

		}
		map = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.fragment_map_view_ui)).getMap();

		setLoadTilesView();
		setEditFeaturesView();

		touch = new TouchableMap(getActivity());
		touch.addView(view);

		map.setMyLocationEnabled(true);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		int mapType = settings.getInt(MAP_TYPE_KEY, 1);
		map.setMapType(mapType);

		map.setOnMapLongClickListener(this);
		map.setOnMapClickListener(this);
		map.setOnMarkerClickListener(this);
		map.setOnMarkerDragListener(this);

		manager = GeoPackageFactory.getManager(getActivity());
		updateInBackground();

		return touch;
	}

	/**
	 * Set the load tiles view and buttons
	 */
	private void setLoadTilesView() {
		loadTilesView = view.findViewById(R.id.mapLoadTilesButtons);
		ImageButton loadTilesButton = (ImageButton) loadTilesView
				.findViewById(R.id.mapLoadTilesButton);
		loadTilesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				createTiles();
			}
		});
		boundingBoxClearButton = (ImageButton) loadTilesView
				.findViewById(R.id.mapLoadTilesClearButton);
		boundingBoxClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				clearBoundingBox();
			}
		});
	}

	/**
	 * Set the edit features view and buttons
	 */
	private void setEditFeaturesView() {
		editFeaturesView = view.findViewById(R.id.mapFeaturesButtons);

		editPointButton = (ImageButton) editFeaturesView
				.findViewById(R.id.mapEditPointButton);
		editPointButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				validateAndClearEditFeatures(EditType.POINT);
			}
		});

		editLinestringButton = (ImageButton) editFeaturesView
				.findViewById(R.id.mapEditLinestringButton);
		editLinestringButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				validateAndClearEditFeatures(EditType.LINESTRING);
			}
		});

		editPolygonButton = (ImageButton) editFeaturesView
				.findViewById(R.id.mapEditPolygonButton);
		editPolygonButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				validateAndClearEditFeatures(EditType.POLYGON);
			}
		});

		editAcceptButton = (ImageButton) editFeaturesView
				.findViewById(R.id.mapEditAcceptButton);
		editAcceptButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {

				if (editFeatureType != null && !editPoints.isEmpty()) {
					boolean accept = false;
					switch (editFeatureType) {
					case POINT:
						accept = true;
						break;
					case LINESTRING:
						if (editPoints.size() >= 2) {
							accept = true;
						}
						break;
					case POLYGON:
						if (editPoints.size() >= 3) {
							accept = true;
						}
						break;
					}
					if (accept) {
						saveEditFeatures();
					}
				}
			}
		});

		editClearButton = (ImageButton) editFeaturesView
				.findViewById(R.id.mapEditClearButton);
		editClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!editPoints.isEmpty()) {
					clearEditFeaturesAndPreserveType();
				}
			}
		});

	}

	/**
	 * If there are unsaved edits prompt the user for validation. Clear edit
	 * features if ok.
	 * 
	 * @param editTypeClicked
	 */
	private void validateAndClearEditFeatures(final EditType editTypeClicked) {

		if (editPoints.isEmpty()) {
			clearEditFeaturesAndUpdateType(editTypeClicked);
		} else {

			AlertDialog deleteDialog = new AlertDialog.Builder(getActivity())
					.setTitle(
							getString(R.string.edit_features_clear_validation_label))
					.setMessage(
							getString(R.string.edit_features_clear_validation_message))
					.setPositiveButton(getString(R.string.button_ok_label),

					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							clearEditFeaturesAndUpdateType(editTypeClicked);
						}
					})

					.setNegativeButton(getString(R.string.button_cancel_label),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).create();
			deleteDialog.show();
		}
	}

	/**
	 * Clear edit features and update the type
	 * 
	 * @param editType
	 */
	private void clearEditFeaturesAndUpdateType(EditType editType) {
		EditType previousType = editFeatureType;
		clearEditFeatures();
		setEditType(previousType, editType);
	}

	/**
	 * Clear edit features and preserve type
	 */
	private void clearEditFeaturesAndPreserveType() {
		EditType previousType = editFeatureType;
		clearEditFeatures();
		setEditType(null, previousType);
	}

	/**
	 * Set the edit type
	 * 
	 * @param editType
	 */
	private void setEditType(EditType previousType, EditType editType) {

		if (editType != null && previousType != editType) {

			editFeatureType = editType;
			switch (editType) {
			case POINT:
				editPointButton
						.setImageResource(R.drawable.ic_edit_point_active);
				break;
			case LINESTRING:
				editLinestringButton
						.setImageResource(R.drawable.ic_edit_linestring_active);
				break;
			case POLYGON:
				editPolygonButton
						.setImageResource(R.drawable.ic_edit_polygon_active);
				break;
			}
		}
	}

	/**
	 * Save the edit features
	 */
	private void saveEditFeatures() {

		boolean changesMade = false;

		GeoPackage geoPackage = manager.open(editFeaturesDatabase);
		try {
			FeatureDao featureDao = geoPackage.getFeatureDao(editFeaturesTable);
			long srsId = featureDao.getGeometryColumns().getSrsId();

			GoogleMapShapeConverter converter = new GoogleMapShapeConverter();

			switch (editFeatureType) {
			case POINT:

				for (Marker pointMarker : editPoints.values()) {
					mil.nga.giat.geopackage.geom.Point point = converter
							.toPoint(pointMarker.getPosition());
					FeatureRow newPoint = featureDao.newRow();
					GeoPackageGeometryData pointGeomData = new GeoPackageGeometryData(
							srsId);
					pointGeomData.setGeometry(point);
					newPoint.setGeometry(pointGeomData);
					featureDao.insert(newPoint);
				}
				changesMade = true;
				break;

			case LINESTRING:

				LineString lineString = converter.toLineString(editLinestring);
				FeatureRow newLineString = featureDao.newRow();
				GeoPackageGeometryData lineStringGeomData = new GeoPackageGeometryData(
						srsId);
				lineStringGeomData.setGeometry(lineString);
				newLineString.setGeometry(lineStringGeomData);
				featureDao.insert(newLineString);
				changesMade = true;
				break;

			case POLYGON:

				mil.nga.giat.geopackage.geom.Polygon polygon = converter
						.toPolygon(editPolygon);
				FeatureRow newPolygon = featureDao.newRow();
				GeoPackageGeometryData polygonGeomData = new GeoPackageGeometryData(
						srsId);
				polygonGeomData.setGeometry(polygon);
				newPolygon.setGeometry(polygonGeomData);
				featureDao.insert(newPolygon);
				changesMade = true;
				break;
			}
		} catch (Exception e) {
			if (GeoPackageUtils.isFutureSQLiteException(e)) {
				GeoPackageUtils
						.showMessage(
								getActivity(),
								getString(R.string.edit_features_save_label)
										+ " " + editFeatureType.name(),
								"GeoPackage was created using a more recent SQLite version unsupported by Android");
			} else {
				GeoPackageUtils.showMessage(getActivity(),
						getString(R.string.edit_features_save_label) + " "
								+ editFeatureType.name(), e.getMessage());
			}
		} finally {
			geoPackage.close();
		}

		clearEditFeaturesAndPreserveType();

		if (changesMade) {
			active.setModified(true);
			updateInBackground();
		}

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public View getView() {
		return view;
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		map.setMyLocationEnabled(!hidden);

		if (!hidden && active.isModified()) {
			active.setModified(false);
			updateInBackground();
		}
	}

	/**
	 * Handle the menu reset
	 * 
	 * @param menu
	 */
	public void handleMenu(Menu menu) {
		resetBoundingBox();
		resetEditFeatures();
	}

	/**
	 * Handle map menu clicks
	 * 
	 * @param item
	 * @return
	 */
	public boolean handleMenuClick(MenuItem item) {

		boolean handled = true;

		switch (item.getItemId()) {
		case R.id.map_features:
			editFeaturesMenuItem = item;
			if (!editFeaturesMode) {
				selectEditFeatures();
			} else {
				resetEditFeatures();
			}
			break;
		case R.id.map_bounding_box:
			boundingBoxMenuItem = item;
			if (!boundingBoxMode) {

				if (editFeaturesMode) {
					resetEditFeatures();
				}

				boundingBoxMode = true;
				loadTilesView.setVisibility(View.VISIBLE);
				boundingBoxMenuItem.setIcon(R.drawable.ic_bounding_box_active);
			} else {
				resetBoundingBox();
			}
			break;
		case R.id.max_features:
			setMaxFeatures();
			break;
		case R.id.normal_map:
			setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
		case R.id.satellite_map:
			setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			break;
		case R.id.terrain_map:
			setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			break;
		case R.id.hybrid_map:
			setMapType(GoogleMap.MAP_TYPE_HYBRID);
			break;
		default:
			handled = false;
			break;
		}

		return handled;
	}

	/**
	 * Select the features to edit
	 */
	private void selectEditFeatures() {

		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View editFeaturesSelectionView = inflater.inflate(
				R.layout.edit_features_selection, null);
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		dialog.setView(editFeaturesSelectionView);

		final Spinner geoPackageInput = (Spinner) editFeaturesSelectionView
				.findViewById(R.id.edit_features_selection_geopackage);
		final Spinner featuresInput = (Spinner) editFeaturesSelectionView
				.findViewById(R.id.edit_features_selection_features);

		List<String> databases = manager.databases();
		List<String> featureDatabases = new ArrayList<String>();
		if (databases != null) {
			for (String database : databases) {
				GeoPackage geoPackage = manager.open(database);
				if (!geoPackage.getFeatureTables().isEmpty()) {
					featureDatabases.add(database);
				}
				geoPackage.close();
			}
		}
		if (featureDatabases.isEmpty()) {
			GeoPackageUtils.showMessage(getActivity(),
					getString(R.string.edit_features_selection_features_label),
					"No GeoPackages with features to edit");
			return;
		}
		ArrayAdapter<String> geoPackageAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_item,
				featureDatabases);
		geoPackageInput.setAdapter(geoPackageAdapter);

		updateFeaturesSelection(featuresInput, featureDatabases.get(0));

		geoPackageInput
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView,
							View selectedItemView, int position, long id) {
						String geoPackage = geoPackageInput.getSelectedItem()
								.toString();
						updateFeaturesSelection(featuresInput, geoPackage);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {
					}
				});

		dialog.setPositiveButton(getString(R.string.button_ok_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {

						try {

							if (boundingBoxMode) {
								resetBoundingBox();
							}

							editFeaturesDatabase = geoPackageInput
									.getSelectedItem().toString();
							editFeaturesTable = featuresInput.getSelectedItem()
									.toString();

							editFeaturesMode = true;
							editFeaturesView.setVisibility(View.VISIBLE);
							editFeaturesMenuItem
									.setIcon(R.drawable.ic_features_active);

							updateInBackground();

						} catch (Exception e) {
							GeoPackageUtils
									.showMessage(
											getActivity(),
											getString(R.string.edit_features_selection_features_label),
											e.getMessage());
						}
					}
				}).setNegativeButton(getString(R.string.button_cancel_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		dialog.show();

	}

	/**
	 * Update the features selection based upon the database
	 * 
	 * @param featuresInput
	 * @param database
	 */
	private void updateFeaturesSelection(Spinner featuresInput, String database) {

		GeoPackage geoPackage = manager.open(database);
		List<String> features = geoPackage.getFeatureTables();
		ArrayAdapter<String> featuresAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_item, features);
		featuresInput.setAdapter(featuresAdapter);
	}

	/**
	 * Reset the bounding box mode
	 */
	private void resetBoundingBox() {
		boundingBoxMode = false;
		loadTilesView.setVisibility(View.INVISIBLE);
		if (boundingBoxMenuItem != null) {
			boundingBoxMenuItem.setIcon(R.drawable.ic_bounding_box);
		}
		clearBoundingBox();
	}

	/**
	 * Reset the edit features state
	 */
	private void resetEditFeatures() {
		editFeaturesMode = false;
		editFeaturesView.setVisibility(View.INVISIBLE);
		if (editFeaturesMenuItem != null) {
			editFeaturesMenuItem.setIcon(R.drawable.ic_features);
		}
		editFeaturesDatabase = null;
		editFeaturesTable = null;
		editFeatureIds.clear();
		clearEditFeatures();
		updateInBackground();
	}

	/**
	 * Turn off the loading of tiles
	 */
	private void clearBoundingBox() {
		if (boundingBoxClearButton != null) {
			boundingBoxClearButton.setImageResource(R.drawable.ic_clear);
		}
		if (boundingBox != null) {
			boundingBox.remove();
		}
		boundingBoxStartCorner = null;
		boundingBoxEndCorner = null;
		boundingBox = null;
		setDrawing(false);
	}

	/**
	 * Clear the edit features
	 */
	private void clearEditFeatures() {
		editFeatureType = null;
		for (Marker editMarker : editPoints.values()) {
			editMarker.remove();
		}
		editPoints.clear();
		if (editLinestring != null) {
			editLinestring.remove();
			editLinestring = null;
		}
		if (editPolygon != null) {
			editPolygon.remove();
			editPolygon = null;
		}
		editPointButton.setImageResource(R.drawable.ic_edit_point);
		editLinestringButton.setImageResource(R.drawable.ic_edit_linestring);
		editPolygonButton.setImageResource(R.drawable.ic_edit_polygon);
		editAcceptButton.setImageResource(R.drawable.ic_accept);
		editClearButton.setImageResource(R.drawable.ic_clear);
	}

	/**
	 * Let the user set the max number of features to draw
	 */
	private void setMaxFeatures() {

		final EditText input = new EditText(getActivity());
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		final String maxFeatures = String.valueOf(getMaxFeatures());
		input.setText(maxFeatures);

		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
				.setTitle(getString(R.string.map_max_features))
				.setMessage(getString(R.string.map_max_features_message))
				.setView(input)
				.setPositiveButton(getString(R.string.button_ok_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String value = input.getText().toString();
								if (value != null && !value.equals(maxFeatures)) {
									int maxFeature = Integer.parseInt(value);
									SharedPreferences settings = PreferenceManager
											.getDefaultSharedPreferences(getActivity());
									Editor editor = settings.edit();
									editor.putInt(MAX_FEATURES_KEY, maxFeature);
									editor.commit();
									updateInBackground();
								}
							}
						})
				.setNegativeButton(getString(R.string.button_cancel_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.cancel();
							}
						});

		dialog.show();
	}

	/**
	 * Set the map type
	 * 
	 * @param mapType
	 */
	private void setMapType(int mapType) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		Editor editor = settings.edit();
		editor.putInt(MAP_TYPE_KEY, mapType);
		editor.commit();
		if (map != null) {
			map.setMapType(mapType);
		}
	}

	/**
	 * Update the map by kicking off a background task
	 */
	private void updateInBackground() {

		if (updateTask != null) {
			updateTask.cancel(false);
		}
		map.clear();
		for (GeoPackage geoPackage : geoPackages.values()) {
			geoPackage.close();
		}
		geoPackages.clear();
		updateTask = new MapUpdateTask();
		updateTask.execute();

	}

	/**
	 * Update the map in the background
	 */
	private class MapUpdateTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			update(this);
			return null;
		}
	}

	/**
	 * Get the max features
	 * 
	 * @return
	 */
	private int getMaxFeatures() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		int maxFeatures = settings.getInt(MAX_FEATURES_KEY, getResources()
				.getInteger(R.integer.map_max_features_default));
		return maxFeatures;
	}

	/**
	 * Update the map
	 * 
	 * @param task
	 */
	private void update(MapUpdateTask task) {

		if (active != null) {

			// Add tile overlays first
			for (GeoPackageDatabase database : active.getDatabases()) {

				// Open each GeoPackage
				GeoPackage geoPackage = manager.open(database.getDatabase());
				geoPackages.put(database.getDatabase(), geoPackage);

				// Display the tiles
				for (GeoPackageTable tiles : database.getTiles()) {
					displayTiles(task, tiles);
					if (task.isCancelled()) {
						break;
					}
				}

				if (task.isCancelled()) {
					break;
				}
			}

			// Add features
			int featuresLeft = getMaxFeatures();
			Map<String, List<String>> featureTables = new HashMap<String, List<String>>();
			if (editFeaturesMode) {
				List<String> databaseFeatures = new ArrayList<String>();
				databaseFeatures.add(editFeaturesTable);
				featureTables.put(editFeaturesDatabase, databaseFeatures);
				GeoPackage geoPackage = geoPackages.get(editFeaturesDatabase);
				if (geoPackage == null) {
					geoPackage = manager.open(editFeaturesDatabase);
					geoPackages.put(editFeaturesDatabase, geoPackage);
				}
			} else {
				for (GeoPackageDatabase database : active.getDatabases()) {
					if (!database.getFeatures().isEmpty()) {
						List<String> databaseFeatures = new ArrayList<String>();
						featureTables.put(database.getDatabase(),
								databaseFeatures);
						for (GeoPackageTable features : database.getFeatures()) {
							databaseFeatures.add(features.getName());
						}
					}
				}
			}
			for (Map.Entry<String, List<String>> databaseFeatures : featureTables
					.entrySet()) {

				if (featuresLeft <= 0) {
					break;
				}

				for (String features : databaseFeatures.getValue()) {
					int count = displayFeatures(task,
							databaseFeatures.getKey(), features, featuresLeft,
							editFeaturesMode);
					featuresLeft -= count;
					if (task.isCancelled() || featuresLeft <= 0) {
						break;
					}
				}

				if (task.isCancelled()) {
					break;
				}
			}

			if (featuresLeft <= 0) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity(),
								"Max Features Drawn: " + getMaxFeatures(),
								Toast.LENGTH_SHORT).show();
					}
				});
			}

		}

	}

	/**
	 * Display features
	 * 
	 * @param task
	 * @param database
	 * @param features
	 * @param maxFeatures
	 * @param editable
	 * @return count of features added
	 */
	private int displayFeatures(MapUpdateTask task, String database,
			String features, int maxFeatures, final boolean editable) {

		int count = 0;

		GeoPackage geoPackage = geoPackages.get(database);

		FeatureDao featureDao = geoPackage.getFeatureDao(features);

		FeatureCursor cursor = featureDao.queryForAll();
		try {

			final GoogleMapShapeConverter converter = new GoogleMapShapeConverter();

			while (!task.isCancelled() && count < maxFeatures
					&& cursor.moveToNext()) {
				final FeatureRow row = cursor.getRow();
				GeoPackageGeometryData geometryData = row.getGeometry();
				if (geometryData != null && !geometryData.isEmpty()) {

					final Geometry geometry = geometryData.getGeometry();

					if (geometry != null) {
						count++;
						final Object shape = converter.toShape(geometry);
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Object mapShape = converter.addShapeToMap(map,
										shape);

								if (editable) {
									addEditableShape(row, mapShape);
								}
							}
						});
					}
				}
			}

		} finally {
			cursor.close();
		}

		return count;
	}

	/**
	 * Add editable shape
	 * 
	 * @param row
	 * @param shape
	 */
	private void addEditableShape(FeatureRow row, Object shape) {

		if (shape instanceof Marker) {
			Marker marker = (Marker) shape;
			editFeatureIds.put(marker.getId(), row.getId());
		} else if (shape instanceof Polyline) {
			Polyline polyline = (Polyline) shape;
			MarkerOptions polylineMarkerOptions = new MarkerOptions();
			polylineMarkerOptions.position(polyline.getPoints().get(0));
			Marker polylineMarker = map.addMarker(polylineMarkerOptions);
			editFeatureIds.put(polylineMarker.getId(), row.getId());
			// TODO save the polyline
		} else if (shape instanceof Polygon) {
			Polygon polygon = (Polygon) shape;
			MarkerOptions polygonMarkerOptions = new MarkerOptions();
			polygonMarkerOptions.position(polygon.getPoints().get(0));
			Marker polygonMarker = map.addMarker(polygonMarkerOptions);
			editFeatureIds.put(polygonMarker.getId(), row.getId());
			// TODO save the polygon
		}
		// Currently not supporting multi type geometries

	}

	/**
	 * Display tiles
	 * 
	 * @param task
	 * @param tiles
	 */
	private void displayTiles(MapUpdateTask task, GeoPackageTable tiles) {

		GeoPackage geoPackage = geoPackages.get(tiles.getDatabase());

		TileDao tileDao = geoPackage.getTileDao(tiles.getName());

		GoogleAPIGeoPackageOverlay overlay = new GoogleAPIGeoPackageOverlay(
				tileDao);
		final TileOverlayOptions overlayOptions = new TileOverlayOptions();
		overlayOptions.tileProvider(overlay);
		overlayOptions.zIndex(-1);

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				map.addTileOverlay(overlayOptions);
			}
		});
	}

	@Override
	public void onMapLongClick(LatLng point) {

		if (boundingBoxMode) {

			vibrator.vibrate(getActivity().getResources().getInteger(
					R.integer.map_tiles_long_click_vibrate));

			// Check to see if editing any of the bounding box corners
			if (boundingBox != null && boundingBoxEndCorner != null) {
				Projection projection = map.getProjection();

				double allowableScreenPercentage = (getActivity()
						.getResources()
						.getInteger(
								R.integer.map_tiles_long_click_screen_percentage) / 100.0);
				Point screenPoint = projection.toScreenLocation(point);

				if (isWithinDistance(projection, screenPoint,
						boundingBoxEndCorner, allowableScreenPercentage)) {
					setDrawing(true);
				} else if (isWithinDistance(projection, screenPoint,
						boundingBoxStartCorner, allowableScreenPercentage)) {
					LatLng temp = boundingBoxStartCorner;
					boundingBoxStartCorner = boundingBoxEndCorner;
					boundingBoxEndCorner = temp;
					setDrawing(true);
				} else {
					LatLng corner1 = new LatLng(
							boundingBoxStartCorner.latitude,
							boundingBoxEndCorner.longitude);
					LatLng corner2 = new LatLng(boundingBoxEndCorner.latitude,
							boundingBoxStartCorner.longitude);
					if (isWithinDistance(projection, screenPoint, corner1,
							allowableScreenPercentage)) {
						boundingBoxStartCorner = corner2;
						boundingBoxEndCorner = corner1;
						setDrawing(true);
					} else if (isWithinDistance(projection, screenPoint,
							corner2, allowableScreenPercentage)) {
						boundingBoxStartCorner = corner1;
						boundingBoxEndCorner = corner2;
						setDrawing(true);
					}
				}
			}

			// Start drawing a new polygon
			if (!drawing) {
				if (boundingBox != null) {
					boundingBox.remove();
				}
				boundingBoxStartCorner = point;
				boundingBoxEndCorner = point;
				PolygonOptions polygonOptions = new PolygonOptions();
				List<LatLng> points = getPolygonPoints(boundingBoxStartCorner,
						boundingBoxEndCorner);
				polygonOptions.addAll(points);
				boundingBox = map.addPolygon(polygonOptions);
				setDrawing(true);
				if (boundingBoxClearButton != null) {
					boundingBoxClearButton
							.setImageResource(R.drawable.ic_clear_active);
				}
			}
		} else if (editFeatureType != null) {
			vibrator.vibrate(getActivity().getResources().getInteger(
					R.integer.edit_features_add_long_click_vibrate));
			addEditPoint(point);
			updateEditState(true);
		}
	}

	/**
	 * Add edit point
	 * 
	 * @param point
	 */
	private void addEditPoint(LatLng point) {
		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(point);
		markerOptions.draggable(true);
		Marker marker = map.addMarker(markerOptions);
		editPoints.put(marker.getId(), marker);
	}

	/**
	 * Update the current edit state, buttons, and visuals
	 * 
	 * @param numberPointsChanged
	 */
	private void updateEditState(boolean numberPointsChanged) {
		boolean accept = false;
		switch (editFeatureType) {

		case POINT:
			accept = true;
			break;

		case LINESTRING:

			if (editPoints.size() >= 2) {
				accept = true;

				List<LatLng> points = getEditLatLngPoints();
				if (editLinestring != null) {
					editLinestring.setPoints(points);
				} else {
					PolylineOptions polylineOptions = new PolylineOptions();
					polylineOptions.addAll(points);
					editLinestring = map.addPolyline(polylineOptions);
				}
			} else if (editLinestring != null) {
				editLinestring.remove();
				editLinestring = null;
			}

			break;

		case POLYGON:

			if (editPoints.size() >= 3) {
				accept = true;

				List<LatLng> points = getEditLatLngPoints();
				if (editPolygon != null) {
					editPolygon.setPoints(points);
				} else {
					PolygonOptions polygonOptions = new PolygonOptions();
					polygonOptions.addAll(points);
					editPolygon = map.addPolygon(polygonOptions);
				}
			} else if (editPolygon != null) {
				editPolygon.remove();
				editPolygon = null;
			}

			break;
		}

		if (numberPointsChanged) {
			editClearButton.setImageResource(R.drawable.ic_clear_active);
			if (accept) {
				editAcceptButton.setImageResource(R.drawable.ic_accept_active);
			}
		}
	}

	/**
	 * Get a list of points as LatLng
	 * 
	 * @return
	 */
	private List<LatLng> getEditLatLngPoints() {
		List<LatLng> points = new ArrayList<LatLng>();
		for (Marker editPoint : editPoints.values()) {
			points.add(editPoint.getPosition());
		}
		return points;
	}

	/**
	 * Set the drawing value
	 * 
	 * @param drawing
	 */
	private void setDrawing(boolean drawing) {
		this.drawing = drawing;
		map.getUiSettings().setScrollGesturesEnabled(!drawing);
	}

	/**
	 * Check if the point is within clicking distance to the lat lng corner
	 * 
	 * @param projection
	 * @param point
	 * @param corner
	 * @param allowableScreenPercentage
	 * @return
	 */
	private boolean isWithinDistance(Projection projection, Point point,
			LatLng latLng, double allowableScreenPercentage) {
		Point point2 = projection.toScreenLocation(latLng);
		double distance = Math.sqrt(Math.pow(point.x - point2.x, 2)
				+ Math.pow(point.y - point2.y, 2));

		boolean withinDistance = distance
				/ Math.min(view.getWidth(), view.getHeight()) <= allowableScreenPercentage;
		return withinDistance;
	}

	@Override
	public void onMapClick(LatLng point) {

	}

	@Override
	public boolean onMarkerClick(Marker marker) {

		if (editFeaturesMode) {
			Long featureId = editFeatureIds.get(marker.getId());
			if (featureId != null) {
				editExistingFeatureClick(marker, featureId);
			} else {
				Marker editPoint = editPoints.get(marker.getId());
				if (editPoint != null) {
					editMarkerClick(marker);
				}
			}
		}
		return false;
	}

	@Override
	public void onMarkerDrag(Marker marker) {
		updateEditState(false);
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		updateEditState(false);
	}

	@Override
	public void onMarkerDragStart(Marker marker) {
		vibrator.vibrate(getActivity().getResources().getInteger(
				R.integer.edit_features_drag_long_click_vibrate));
	}

	/**
	 * Edit marker click
	 * 
	 * @param marker
	 */
	private void editMarkerClick(final Marker marker) {

		// TODO determine if this is a polyline or polygon

		LatLng position = marker.getPosition();
		String message = editFeatureType.name();
		if (editFeatureType != EditType.POINT) {
			message += " " + EditType.POINT.name();
		}
		AlertDialog deleteDialog = new AlertDialog.Builder(getActivity())
				.setCancelable(false)
				.setTitle(getString(R.string.edit_features_delete_label))
				.setMessage(
						getString(R.string.edit_features_delete_label) + " "
								+ message + " (lat=" + position.latitude
								+ ", lon=" + position.longitude + ") ?")
				.setPositiveButton(
						getString(R.string.edit_features_delete_label),

						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								editPoints.remove(marker.getId());
								marker.remove();

								updateEditState(true);

							}
						})

				.setNegativeButton(getString(R.string.button_cancel_label),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create();
		deleteDialog.show();
	}

	/**
	 * Edit existing feature click
	 * 
	 * @param marker
	 * @param featureId
	 */
	private void editExistingFeatureClick(final Marker marker, long featureId) {
		final GeoPackage geoPackage = manager.open(editFeaturesDatabase);
		final FeatureDao featureDao = geoPackage
				.getFeatureDao(editFeaturesTable);

		final FeatureRow featureRow = featureDao.queryForIdRow(featureId);

		if (featureRow != null) {
			GeoPackageGeometryData geomData = featureRow.getGeometry();
			final GeometryType geometryType = geomData.getGeometry()
					.getGeometryType();

			LatLng position = marker.getPosition();
			AlertDialog deleteDialog = new AlertDialog.Builder(getActivity())
					.setCancelable(false)
					.setTitle(
							getString(R.string.edit_features_delete_label)
									+ " " + geometryType.getName())
					.setMessage(
							getString(R.string.edit_features_delete_label)
									+ " " + geometryType.getName() + " from "
									+ editFeaturesDatabase + " - "
									+ editFeaturesTable + " (lat="
									+ position.latitude + ", lon="
									+ position.longitude + ") ?")
					.setPositiveButton(
							getString(R.string.edit_features_delete_label),

							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									try {
										featureDao.delete(featureRow);
										marker.remove();
										active.setModified(true);
									} catch (Exception e) {
										if (GeoPackageUtils
												.isFutureSQLiteException(e)) {
											GeoPackageUtils
													.showMessage(
															getActivity(),
															getString(R.string.edit_features_delete_label)
																	+ " "
																	+ geometryType
																			.getName(),
															"GeoPackage was created using a more recent SQLite version unsupported by Android");
										} else {
											GeoPackageUtils
													.showMessage(
															getActivity(),
															getString(R.string.edit_features_delete_label)
																	+ " "
																	+ geometryType
																			.getName(),
															e.getMessage());
										}
									} finally {
										geoPackage.close();
									}
								}
							})

					.setNegativeButton(getString(R.string.button_cancel_label),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									geoPackage.close();
									dialog.dismiss();
								}
							}).create();
			deleteDialog.show();
		} else {
			geoPackage.close();
		}
	}

	/**
	 * Get a list of the polygon points for the bounding box
	 * 
	 * @param point1
	 * @param point2
	 * @return
	 */
	private List<LatLng> getPolygonPoints(LatLng point1, LatLng point2) {
		List<LatLng> points = new ArrayList<LatLng>();
		points.add(new LatLng(point1.latitude, point1.longitude));
		points.add(new LatLng(point1.latitude, point2.longitude));
		points.add(new LatLng(point2.latitude, point2.longitude));
		points.add(new LatLng(point2.latitude, point1.longitude));
		return points;
	}

	/**
	 * Touchable map layout
	 * 
	 * @author osbornb
	 */
	public class TouchableMap extends FrameLayout {

		public TouchableMap(Context context) {
			super(context);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				if (boundingBoxMode) {
					if (drawing && boundingBox != null) {
						Point point = new Point((int) ev.getX(),
								(int) ev.getY());
						boundingBoxEndCorner = map.getProjection()
								.fromScreenLocation(point);
						List<LatLng> points = getPolygonPoints(
								boundingBoxStartCorner, boundingBoxEndCorner);
						boundingBox.setPoints(points);
					}
					if (ev.getAction() == MotionEvent.ACTION_UP) {
						setDrawing(false);
					}
				}
				break;
			}
			return super.dispatchTouchEvent(ev);
		}

	}

	/**
	 * Create tiles
	 */
	private void createTiles() {

		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View createTilesView = inflater
				.inflate(R.layout.map_create_tiles, null);
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		dialog.setView(createTilesView);

		final EditText geopackageInput = (EditText) createTilesView
				.findViewById(R.id.map_create_tiles_geopackage_input);
		final Button geopackagesButton = (Button) createTilesView
				.findViewById(R.id.map_create_tiles_preloaded);
		final EditText nameInput = (EditText) createTilesView
				.findViewById(R.id.create_tiles_name_input);
		final EditText urlInput = (EditText) createTilesView
				.findViewById(R.id.load_tiles_url_input);
		final Button preloadedUrlsButton = (Button) createTilesView
				.findViewById(R.id.load_tiles_preloaded);
		final EditText minZoomInput = (EditText) createTilesView
				.findViewById(R.id.load_tiles_min_zoom_input);
		final EditText maxZoomInput = (EditText) createTilesView
				.findViewById(R.id.load_tiles_max_zoom_input);
		final Spinner compressFormatInput = (Spinner) createTilesView
				.findViewById(R.id.load_tiles_compress_format);
		final EditText compressQualityInput = (EditText) createTilesView
				.findViewById(R.id.load_tiles_compress_quality);
		final EditText minLatInput = (EditText) createTilesView
				.findViewById(R.id.bounding_box_min_latitude_input);
		final EditText maxLatInput = (EditText) createTilesView
				.findViewById(R.id.bounding_box_max_latitude_input);
		final EditText minLonInput = (EditText) createTilesView
				.findViewById(R.id.bounding_box_min_longitude_input);
		final EditText maxLonInput = (EditText) createTilesView
				.findViewById(R.id.bounding_box_max_longitude_input);
		final Button preloadedLocationsButton = (Button) createTilesView
				.findViewById(R.id.bounding_box_preloaded);

		GeoPackageUtils
				.prepareBoundingBoxInputs(getActivity(), minLatInput,
						maxLatInput, minLonInput, maxLonInput,
						preloadedLocationsButton);

		GeoPackageUtils.prepareTileLoadInputs(getActivity(), minZoomInput,
				maxZoomInput, preloadedUrlsButton, nameInput, urlInput,
				compressFormatInput, compressQualityInput);

		if (boundingBox != null) {
			double minLat = 90.0;
			double minLon = 180.0;
			double maxLat = -90.0;
			double maxLon = -180.0;
			for (LatLng point : boundingBox.getPoints()) {
				minLat = Math.min(minLat, point.latitude);
				minLon = Math.min(minLon, point.longitude);
				maxLat = Math.max(maxLat, point.latitude);
				maxLon = Math.max(maxLon, point.longitude);
			}
			minLatInput.setText(String.valueOf(minLat));
			maxLatInput.setText(String.valueOf(maxLat));
			minLonInput.setText(String.valueOf(minLon));
			maxLonInput.setText(String.valueOf(maxLon));
		}

		geopackagesButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						getActivity(), android.R.layout.select_dialog_item);
				final List<String> databases = manager.databases();
				adapter.addAll(databases);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setTitle(getActivity()
						.getString(
								R.string.map_create_tiles_existing_geopackage_dialog_label));
				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								if (item >= 0) {
									String database = databases.get(item);
									geopackageInput.setText(database);
								}
							}
						});

				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		dialog.setPositiveButton(
				getString(R.string.geopackage_create_tiles_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {

						try {

							String database = geopackageInput.getText()
									.toString();
							if (database == null || database.isEmpty()) {
								throw new GeoPackageException(
										getString(R.string.map_create_tiles_geopackage_label)
												+ " is required");
							}
							String tableName = nameInput.getText().toString();
							if (tableName == null || tableName.isEmpty()) {
								throw new GeoPackageException(
										getString(R.string.create_tiles_name_label)
												+ " is required");
							}
							String tileUrl = urlInput.getText().toString();
							int minZoom = Integer.valueOf(minZoomInput
									.getText().toString());
							int maxZoom = Integer.valueOf(maxZoomInput
									.getText().toString());
							double minLat = Double.valueOf(minLatInput
									.getText().toString());
							double maxLat = Double.valueOf(maxLatInput
									.getText().toString());
							double minLon = Double.valueOf(minLonInput
									.getText().toString());
							double maxLon = Double.valueOf(maxLonInput
									.getText().toString());

							if (minLat > maxLat) {
								throw new GeoPackageException(
										getString(R.string.bounding_box_min_latitude_label)
												+ " can not be larger than "
												+ getString(R.string.bounding_box_max_latitude_label));
							}

							if (minLon > maxLon) {
								throw new GeoPackageException(
										getString(R.string.bounding_box_min_longitude_label)
												+ " can not be larger than "
												+ getString(R.string.bounding_box_max_longitude_label));
							}

							CompressFormat compressFormat = null;
							Integer compressQuality = null;
							if (compressFormatInput.getSelectedItemPosition() > 0) {
								compressFormat = CompressFormat
										.valueOf(compressFormatInput
												.getSelectedItem().toString());
								compressQuality = Integer
										.valueOf(compressQualityInput.getText()
												.toString());
							}

							BoundingBox boundingBox = new BoundingBox(minLon,
									maxLon, minLat, maxLat);

							// Create the database if it doesn't exist
							if (!manager.exists(database)) {
								manager.create(database);
							}

							GeoPackageTable table = GeoPackageTable.createTile(
									database, tableName, 0);
							active.addTable(table);

							// Load tiles
							LoadTilesTask.loadTiles(getActivity(),
									GeoPackageMapFragment.this, active,
									database, tableName, tileUrl, minZoom,
									maxZoom, compressFormat, compressQuality,
									boundingBox);
						} catch (Exception e) {
							GeoPackageUtils
									.showMessage(
											getActivity(),
											getString(R.string.geopackage_create_tiles_label),
											e.getMessage());
						}
					}
				}).setNegativeButton(getString(R.string.button_cancel_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		dialog.show();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadTilesCancelled(String result) {
		loadTilesFinished();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLoadTilesPostExecute(String result) {
		loadTilesFinished();
	}

	/**
	 * When loading tiles is finished
	 */
	private void loadTilesFinished() {
		if (active.isModified()) {
			updateInBackground();
			if (boundingBox != null) {
				PolygonOptions polygonOptions = new PolygonOptions();
				polygonOptions.addAll(boundingBox.getPoints());
				boundingBox = map.addPolygon(polygonOptions);
			}
		}
	}

}
