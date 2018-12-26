package com.dadoutek.uled.dao;

import java.util.List;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;

import com.dadoutek.uled.model.DbModel.DbColorNode;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DB_COLOR_NODE".
*/
public class DbColorNodeDao extends AbstractDao<DbColorNode, Long> {

    public static final String TABLENAME = "DB_COLOR_NODE";

    /**
     * Properties of entity DbColorNode.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Index = new Property(1, long.class, "index", false, "INDEX");
        public final static Property Brightness = new Property(2, int.class, "brightness", false, "BRIGHTNESS");
        public final static Property ColorTemperature = new Property(3, int.class, "colorTemperature", false, "COLOR_TEMPERATURE");
        public final static Property Rgbw = new Property(4, int.class, "rgbw", false, "RGBW");
    }

    private Query<DbColorNode> dbDiyGradient_ColorNodesQuery;

    public DbColorNodeDao(DaoConfig config) {
        super(config);
    }
    
    public DbColorNodeDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DB_COLOR_NODE\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"INDEX\" INTEGER NOT NULL ," + // 1: index
                "\"BRIGHTNESS\" INTEGER NOT NULL ," + // 2: brightness
                "\"COLOR_TEMPERATURE\" INTEGER NOT NULL ," + // 3: colorTemperature
                "\"RGBW\" INTEGER NOT NULL );"); // 4: rgbw
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DB_COLOR_NODE\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DbColorNode entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getIndex());
        stmt.bindLong(3, entity.getBrightness());
        stmt.bindLong(4, entity.getColorTemperature());
        stmt.bindLong(5, entity.getRgbw());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DbColorNode entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getIndex());
        stmt.bindLong(3, entity.getBrightness());
        stmt.bindLong(4, entity.getColorTemperature());
        stmt.bindLong(5, entity.getRgbw());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public DbColorNode readEntity(Cursor cursor, int offset) {
        DbColorNode entity = new DbColorNode( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getLong(offset + 1), // index
            cursor.getInt(offset + 2), // brightness
            cursor.getInt(offset + 3), // colorTemperature
            cursor.getInt(offset + 4) // rgbw
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DbColorNode entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setIndex(cursor.getLong(offset + 1));
        entity.setBrightness(cursor.getInt(offset + 2));
        entity.setColorTemperature(cursor.getInt(offset + 3));
        entity.setRgbw(cursor.getInt(offset + 4));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DbColorNode entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DbColorNode entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DbColorNode entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
    /** Internal query to resolve the "colorNodes" to-many relationship of DbDiyGradient. */
    public List<DbColorNode> _queryDbDiyGradient_ColorNodes(long index) {
        synchronized (this) {
            if (dbDiyGradient_ColorNodesQuery == null) {
                QueryBuilder<DbColorNode> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.Index.eq(null));
                dbDiyGradient_ColorNodesQuery = queryBuilder.build();
            }
        }
        Query<DbColorNode> query = dbDiyGradient_ColorNodesQuery.forCurrentThread();
        query.setParameter(0, index);
        return query.list();
    }

}
