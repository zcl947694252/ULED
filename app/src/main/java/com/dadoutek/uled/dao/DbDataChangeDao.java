package com.dadoutek.uled.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.dadoutek.uled.DbModel.DbDataChange;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DB_DATA_CHANGE".
*/
public class DbDataChangeDao extends AbstractDao<DbDataChange, Long> {

    public static final String TABLENAME = "DB_DATA_CHANGE";

    /**
     * Properties of entity DbDataChange.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property ChangeId = new Property(1, Long.class, "changeId", false, "CHANGE_ID");
        public final static Property TableName = new Property(2, String.class, "tableName", false, "TABLE_NAME");
        public final static Property ChangeType = new Property(3, String.class, "changeType", false, "CHANGE_TYPE");
    }


    public DbDataChangeDao(DaoConfig config) {
        super(config);
    }
    
    public DbDataChangeDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DB_DATA_CHANGE\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"CHANGE_ID\" INTEGER," + // 1: changeId
                "\"TABLE_NAME\" TEXT," + // 2: tableName
                "\"CHANGE_TYPE\" TEXT);"); // 3: changeType
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DB_DATA_CHANGE\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DbDataChange entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long changeId = entity.getChangeId();
        if (changeId != null) {
            stmt.bindLong(2, changeId);
        }
 
        String tableName = entity.getTableName();
        if (tableName != null) {
            stmt.bindString(3, tableName);
        }
 
        String changeType = entity.getChangeType();
        if (changeType != null) {
            stmt.bindString(4, changeType);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DbDataChange entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long changeId = entity.getChangeId();
        if (changeId != null) {
            stmt.bindLong(2, changeId);
        }
 
        String tableName = entity.getTableName();
        if (tableName != null) {
            stmt.bindString(3, tableName);
        }
 
        String changeType = entity.getChangeType();
        if (changeType != null) {
            stmt.bindString(4, changeType);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public DbDataChange readEntity(Cursor cursor, int offset) {
        DbDataChange entity = new DbDataChange( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1), // changeId
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // tableName
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3) // changeType
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DbDataChange entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setChangeId(cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1));
        entity.setTableName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setChangeType(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DbDataChange entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DbDataChange entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DbDataChange entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
