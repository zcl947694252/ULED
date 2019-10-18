package com.dadoutek.uled.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.dadoutek.uled.model.DbModel.DbLight;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DB_LIGHT".
*/
public class DbLightDao extends AbstractDao<DbLight, Long> {

    public static final String TABLENAME = "DB_LIGHT";

    /**
     * Properties of entity DbLight.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property MeshAddr = new Property(1, int.class, "meshAddr", false, "MESH_ADDR");
        public final static Property Name = new Property(2, String.class, "name", false, "NAME");
        public final static Property DeviceName = new Property(3, String.class, "deviceName", false, "DEVICE_NAME");
        public final static Property Brightness = new Property(4, int.class, "brightness", false, "BRIGHTNESS");
        public final static Property ColorTemperature = new Property(5, int.class, "colorTemperature", false, "COLOR_TEMPERATURE");
        public final static Property MacAddr = new Property(6, String.class, "macAddr", false, "MAC_ADDR");
        public final static Property MeshUUID = new Property(7, int.class, "meshUUID", false, "MESH_UUID");
        public final static Property ProductUUID = new Property(8, int.class, "productUUID", false, "PRODUCT_UUID");
        public final static Property BelongGroupId = new Property(9, Long.class, "belongGroupId", false, "BELONG_GROUP_ID");
        public final static Property Index = new Property(10, int.class, "index", false, "INDEX");
        public final static Property Color = new Property(11, int.class, "color", false, "COLOR");
        public final static Property Status = new Property(12, int.class, "status", false, "STATUS");
    }


    public DbLightDao(DaoConfig config) {
        super(config);
    }
    
    public DbLightDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DB_LIGHT\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"MESH_ADDR\" INTEGER NOT NULL ," + // 1: meshAddr
                "\"NAME\" TEXT," + // 2: name
                "\"DEVICE_NAME\" TEXT," + // 3: deviceName
                "\"BRIGHTNESS\" INTEGER NOT NULL ," + // 4: brightness
                "\"COLOR_TEMPERATURE\" INTEGER NOT NULL ," + // 5: colorTemperature
                "\"MAC_ADDR\" TEXT," + // 6: macAddr
                "\"MESH_UUID\" INTEGER NOT NULL ," + // 7: meshUUID
                "\"PRODUCT_UUID\" INTEGER NOT NULL ," + // 8: productUUID
                "\"BELONG_GROUP_ID\" INTEGER," + // 9: belongGroupId
                "\"INDEX\" INTEGER NOT NULL ," + // 10: index
                "\"COLOR\" INTEGER NOT NULL ," + // 11: color
                "\"STATUS\" INTEGER NOT NULL );"); // 12: status
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DB_LIGHT\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DbLight entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getMeshAddr());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String deviceName = entity.getDeviceName();
        if (deviceName != null) {
            stmt.bindString(4, deviceName);
        }
        stmt.bindLong(5, entity.getBrightness());
        stmt.bindLong(6, entity.getColorTemperature());
 
        String macAddr = entity.getMacAddr();
        if (macAddr != null) {
            stmt.bindString(7, macAddr);
        }
        stmt.bindLong(8, entity.getMeshUUID());
        stmt.bindLong(9, entity.getProductUUID());
 
        Long belongGroupId = entity.getBelongGroupId();
        if (belongGroupId != null) {
            stmt.bindLong(10, belongGroupId);
        }
        stmt.bindLong(11, entity.getIndex());
        stmt.bindLong(12, entity.getColor());
        stmt.bindLong(13, entity.getStatus());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DbLight entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getMeshAddr());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String deviceName = entity.getDeviceName();
        if (deviceName != null) {
            stmt.bindString(4, deviceName);
        }
        stmt.bindLong(5, entity.getBrightness());
        stmt.bindLong(6, entity.getColorTemperature());
 
        String macAddr = entity.getMacAddr();
        if (macAddr != null) {
            stmt.bindString(7, macAddr);
        }
        stmt.bindLong(8, entity.getMeshUUID());
        stmt.bindLong(9, entity.getProductUUID());
 
        Long belongGroupId = entity.getBelongGroupId();
        if (belongGroupId != null) {
            stmt.bindLong(10, belongGroupId);
        }
        stmt.bindLong(11, entity.getIndex());
        stmt.bindLong(12, entity.getColor());
        stmt.bindLong(13, entity.getStatus());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public DbLight readEntity(Cursor cursor, int offset) {
        DbLight entity = new DbLight( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getInt(offset + 1), // meshAddr
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // name
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // deviceName
            cursor.getInt(offset + 4), // brightness
            cursor.getInt(offset + 5), // colorTemperature
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6), // macAddr
            cursor.getInt(offset + 7), // meshUUID
            cursor.getInt(offset + 8), // productUUID
            cursor.isNull(offset + 9) ? null : cursor.getLong(offset + 9), // belongGroupId
            cursor.getInt(offset + 10), // index
            cursor.getInt(offset + 11), // color
            cursor.getInt(offset + 12) // status
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DbLight entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setMeshAddr(cursor.getInt(offset + 1));
        entity.setName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setDeviceName(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setBrightness(cursor.getInt(offset + 4));
        entity.setColorTemperature(cursor.getInt(offset + 5));
        entity.setMacAddr(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
        entity.setMeshUUID(cursor.getInt(offset + 7));
        entity.setProductUUID(cursor.getInt(offset + 8));
        entity.setBelongGroupId(cursor.isNull(offset + 9) ? null : cursor.getLong(offset + 9));
        entity.setIndex(cursor.getInt(offset + 10));
        entity.setColor(cursor.getInt(offset + 11));
        entity.setStatus(cursor.getInt(offset + 12));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DbLight entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DbLight entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DbLight entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
