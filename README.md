# endeavride-android-user

## installation

1. Clone this repository
2. Open this project by using Android Studio -> File -> New -> Import project, and follow the instructions
3. Change google map API key in `MapDataSource.kt `and `google_maps_api.xml` if needed
4. Currently the server path is pointing to AWS EC2, if you want to change to local server, go to `NetworkUtils.kt` and follow the comments to change `FuelManager.instance.basePath`
5. After gradle sync finished, try to run the project on simulator or on a physical device
