package com.wuyang.baseapp.contract;


import com.wuyang.baseapp.base.BaseEntity;
import com.wuyang.baseapp.base.BaseView;
import com.wuyang.baseapp.entity.MineEntity;

import io.reactivex.Observable;

/**
 * Desccribe:
 *
 * @author Created by wuyang on 2019/9/2
 */
public interface MainActivityContract {
    interface Model {

        Observable<BaseEntity<MineEntity>> forexample(String version);
    }

    interface View extends BaseView<MineEntity> {
        void showSuccess(MineEntity mineEntity);
    }

    interface Presenter {
        void pexample(String version);
    }
}
