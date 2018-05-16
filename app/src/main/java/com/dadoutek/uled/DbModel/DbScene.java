package com.dadoutek.uled.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.dao.DbSceneDao;

/**
 * Created by hejiajun on 2018/5/5.
 */

@Entity
public class DbScene{
    @Id(autoincrement = true)
    private Long id;

    private String name;

    @NotNull
    private String belongAccount;

    @ToMany(referencedJoinProperty = "actionId")
    private List<DbSceneActions> dbSceneActions;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1485122994)
    private transient DbSceneDao myDao;

    @Generated(hash = 1730224282)
    public DbScene(Long id, String name, @NotNull String belongAccount) {
        this.id = id;
        this.name = name;
        this.belongAccount = belongAccount;
    }

    @Generated(hash = 662958756)
    public DbScene() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBelongAccount() {
        return this.belongAccount;
    }

    public void setBelongAccount(String belongAccount) {
        this.belongAccount = belongAccount;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 233131991)
    public List<DbSceneActions> getDbSceneActions() {
        if (dbSceneActions == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            DbSceneActionsDao targetDao = daoSession.getDbSceneActionsDao();
            List<DbSceneActions> dbSceneActionsNew = targetDao
                    ._queryDbScene_DbSceneActions(id);
            synchronized (this) {
                if (dbSceneActions == null) {
                    dbSceneActions = dbSceneActionsNew;
                }
            }
        }
        return dbSceneActions;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1970779748)
    public synchronized void resetDbSceneActions() {
        dbSceneActions = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1398896232)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getDbSceneDao() : null;
    }
}