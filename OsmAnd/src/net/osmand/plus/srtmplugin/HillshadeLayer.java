package net.osmand.plus.srtmplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.views.MapTileLayer;

import org.apache.commons.logging.Log;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;

public class HillshadeLayer extends MapTileLayer {

	private final static Log log = PlatformUtil.getLog(HillshadeLayer.class);
	private Map<String, SQLiteTileSource> resources = new LinkedHashMap<String, SQLiteTileSource>(); 
	private final static String HILLSHADE_CACHE = "hillshade.cache";
	private int ZOOM_BOUNDARY = 15;
	
	private QuadTree<String> indexedResources = new QuadTree<String>(new QuadRect(0, 0, 1 << (ZOOM_BOUNDARY+1), 1 << (ZOOM_BOUNDARY+1)), 8, 0.55f);

	public HillshadeLayer(MapActivity activity, SRTMPlugin srtmPlugin) {
		super(false);
		final OsmandApplication app = activity.getMyApplication();
		indexHillshadeFiles(app);
		setAlpha(100);
		setMap(createTileSource(activity));
	}

	private void indexHillshadeFiles(final OsmandApplication app ) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void> () {
			private SQLiteDatabase sqliteDb;
			@Override
			protected Void doInBackground(Void... params) {
				File tilesDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
				sqliteDb = SQLiteDatabase.openOrCreateDatabase(new File(tilesDir, HILLSHADE_CACHE) , null);
				if(sqliteDb.getVersion() == 0) {
					sqliteDb.setVersion(1);
					sqliteDb.execSQL("CREATE TABLE TILE_SOURCES(filename varchar2(256), date_modified int, left int, right int, top int, bottom int)");
				}
				
				Map<String, Long> fileModified = new HashMap<String, Long>();
				Map<String, SQLiteTileSource> rs = readFiles(app, tilesDir, fileModified);
				indexCachedResources(fileModified, rs);
				indexNonCachedResources(fileModified, rs);
				sqliteDb.close();
				resources = rs;
				return null;
			}

			private void indexNonCachedResources(Map<String, Long> fileModified, Map<String, SQLiteTileSource> rs) {
				for(String filename : fileModified.keySet()) {
					try {
						log.info("Indexing hillshade file " + filename);
						ContentValues cv = new ContentValues();
						cv.put("filename", filename);
						cv.put("date_modified", fileModified.get(filename));
						SQLiteTileSource ts = rs.get(filename);
						QuadRect rt = ts.getRectBoundary(ZOOM_BOUNDARY, 1);
						if (rt != null) {
							indexedResources.insert(filename, rt);
							cv.put("left", (int)rt.left);
							cv.put("right",(int) rt.right);
							cv.put("top", (int)rt.top);
							cv.put("bottom",(int) rt.bottom);
							sqliteDb.insert("TILE_SOURCES", null, cv);
						}
					} catch(RuntimeException e){
						log.error(e.getMessage(), e);
					}
				}
			}

			private void indexCachedResources(Map<String, Long> fileModified, Map<String, SQLiteTileSource> rs) {
				Cursor cursor = sqliteDb.rawQuery("SELECT filename, date_modified, left, right, top, bottom FROM TILE_SOURCES", 
						new String[0]);
				if(cursor.moveToFirst()) {
					do {
						String filename = cursor.getString(0);
						long lastModified = cursor.getLong(1);
						Long read = fileModified.get(filename);
						if(rs.containsKey(filename) && read != null && lastModified == read.longValue()) {
							int left = cursor.getInt(2);
							int right = cursor.getInt(3);
							int top = cursor.getInt(4);
							float bottom = cursor.getInt(5);
							indexedResources.insert(filename, new QuadRect(left, top, right, bottom));
							fileModified.remove(filename);
						}
						
					} while(cursor.moveToNext());
					cursor.close();
				}
			}

			private Map<String, SQLiteTileSource> readFiles(final OsmandApplication app, File tilesDir, Map<String, Long> fileModified) {
				Map<String, SQLiteTileSource> rs = new LinkedHashMap<String, SQLiteTileSource>();
				File[] files = tilesDir.listFiles();
				if(files != null) {
					for(File f : files) {
						if(f != null && f.getName().endsWith(IndexConstants.SQLITE_EXT) && 
								f.getName().toLowerCase().startsWith("hillshade")) {
							SQLiteTileSource ts = new SQLiteTileSource(app, f, new ArrayList<TileSourceTemplate>());
							rs.put(f.getName(), ts);
							fileModified.put(f.getName(), f.lastModified());
						}
					}
				}
				return rs;
			}
			
		};
		task.execute();
	}

	private SQLiteTileSource createTileSource(MapActivity activity) {
		return new SQLiteTileSource(activity.getMyApplication(), null, new ArrayList<TileSourceTemplate>()) {
			
			@Override
			protected SQLiteConnection getDatabase() {
				throw new UnsupportedOperationException();
			}
			
			public boolean isLocked() {
				return false;
			};
			
			List<String> getTileSource(int x, int y, int zoom) {
				ArrayList<String> ls = new ArrayList<String>();
				int z = (zoom- ZOOM_BOUNDARY );
				if(z > 0){
					indexedResources.queryInBox(new QuadRect(x >> z, y >> z, (x >> z), (y >> z)), ls);
				} else {
					indexedResources.queryInBox(new QuadRect(x << -z, y << -z, (x+1)<<-z, (y+1)<<-z), ls);
				}
				return ls;
			}
			
			@Override
			public boolean exists(int x, int y, int zoom, boolean exact) {
				return !getTileSource(x, y, zoom).isEmpty();
			}
			
			@Override
			public Bitmap getImage(int x, int y, int zoom) {
				List<String> ts = getTileSource(x, y, zoom);
				for (String t : ts) {
					SQLiteTileSource sqLiteTileSource = resources.get(t);
					if(sqLiteTileSource.exists(x, y, zoom, false)) {
						return sqLiteTileSource.getImage(x, y, zoom);
					}
				}
				for (String t : ts) {
					SQLiteTileSource sqLiteTileSource = resources.get(t);
					Bitmap img = sqLiteTileSource.getImage(x, y, zoom);
					if (img != null) {
						return img;
					}
				}
				return null;
			}
			
			@Override
			public int getBitDensity() {
				return 32;
			}
			
			@Override
			public int getMinimumZoomSupported() {
				return 5;
			}
			
			@Override
			public int getMaximumZoomSupported() {
				return 19;
			}
			
			@Override
			public int getTileSize() {
				return 256;
			}
			
			@Override
			public boolean couldBeDownloadedFromInternet() {
				return false;
			}
			
			@Override
			public String getName() {
				return "Hillshade";
			}
			
			@Override
			public String getTileFormat() {
				return "jpg";
			}
		};
	}
	

}
