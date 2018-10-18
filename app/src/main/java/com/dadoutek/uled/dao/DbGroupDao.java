package com.dadoutek.uled.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.dadoutek.uled.model.DbModel.DbGroup;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DB_GROUP".
*/
public class DbGroupDao extends AbstractDao<DbGroup, Long> {

    public static final String TABLENAME = "DB_GROUP";

    /**
     * Properties of entity DbGroup.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property MeshAddr = new Property(1, int.class, "meshAddr", false, "MESH_ADDR");
        public final static Property Name = new Property(2, String.class, "name", false, "NAME");
        public final static Property Brightness = new Property(3, int.class, "brightness", false, "BRIGHTNESS");
        public final static Property ColorTemperature = new Property(4, int.class, "colorTemperature", false, "COLOR_TEMPERATURE");
        public final static Property BelongRegionId = new Property(5, int.class, "belongRegionId", false, "BELONG_REGION_ID");
        public final static Property Color = new Property(6, String.class, "color", false, "COLOR");
    }


    public DbGroupDao(DaoConfig config) {
        super(config);
    }
    
    public DbGroupDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DB_GROUP\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"MESH_ADDR\" INTEGER NOT NULL ," + // 1: meshAddr
                "\"NAME\" TEXT," + // 2: name
                "\"BRIGHTNESS\" INTEGER NOT NULL ," + // 3: brightness
                "\"COLOR_TEMPERATURE\" INTEGER NOT NULL ," + // 4: colorTemperature
                "\"BELONG_REGION_ID\" INTEGER NOT NULL ," + // 5: belongRegionId
                "\"COLOR\" TEXT);"); // 6: color
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DB_GROUP\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DbGroup entity) {
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
        stmt.bindLong(4, entity.getBrightness());
        stmt.bindLong(5, entity.getColorTemperature());
        stmt.bindLong(6, entity.getBelongRegionId());
 
        String color = entity.getColor();
        if (color != null) {
            stmt.bindString(7, color);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DbGroup entity) {
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
        stmt.bindLong(4, entity.getBrightness());
        stmt.bindLong(5, entity.getColorTemperature());
        stmt.bindLong(6, entity.getBelongRegionId());
 
        String color = entity.getColor();
        if (color != null) {
            stmt.bindString(7, color);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public DbGroup readEntity(Cursor cursor, int offset) {
        DbGroup entity = new DbGroup( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getInt(offset + 1), // meshAddr
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // name
            cursor.getInt(offset + 3), // brightness
            cursor.getInt(offset + 4), // colorTemperature
            cursor.getInt(offset + 5), // belongRegionId
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6) // color
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DbGroup entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setMeshAddr(cursor.getInt(offset + 1));
        entity.setName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setBrightness(cursor.getInt(offset + 3));
        entity.setColorTemperature(cursor.getInt(offset + 4));
        entity.setBelongRegionId(cursor.getInt(offset + 5));
        entity.setColor(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DbGroup entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DbGroup entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DbGroup entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
