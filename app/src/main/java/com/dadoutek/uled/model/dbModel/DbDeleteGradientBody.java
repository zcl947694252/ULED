package com.dadoutek.uled.model.dbModel;

import java.io.Serializable;
import java.util.List;

public class DbDeleteGradientBody implements Serializable {

    static final long serialVersionUID = -15515456L;

    private List<Integer> idList;

    public List<Integer> getIdList() {
        return idList;
    }

    public void setIdList(List<Integer> idList) {
        this.idList = idList;
    }
}
