package mil.nga.mapcache.view;

/**
 * Listener to be implemented for opening dialog windows
 */
public interface OnDialogButtonClickListener {

    /**
     * Delete GeoPackage
     * @param gpName GeoPackage name
     */
    void onDeleteGP(String gpName);

    /**
     * Show the details dialog of a GeoPackage
     * @param gpName - GeoPackage name
     */
    void onDetailGP(String gpName);

    /**
     * Rename a GeoPackage
     * @param originalName GeoPackage original name
     * @param newName New GeoPackage name
     */
    void onRenameGP(String originalName, String newName);

    /**
     * Share a GeoPackage
     * @param gpName GeoPackage name
     */
    void onShareGP(String gpName);

    /**
     * Copy a GeoPackage
     * @param gpName GeoPackage name
     * @param newName name of the new GeoPackage that you're making
     */
    void onCopyGP(String gpName, String newName);

    /**
     * Cancel button
     */
    void onCancelButtonClicked();
}
