package com.wuyang.baseapp.activity;

import android.view.View;

import com.wuyang.baseapp.R;
import com.wuyang.baseapp.base.BaseActivity;
import com.wuyang.baseapp.contract.MainActivityContract;
import com.wuyang.baseapp.entity.MineEntity;
import com.wuyang.baseapp.presenter.MainActivityPresenter;

/**
 * @author wuyang
 */
public class MainActivity extends BaseActivity<MainActivityPresenter, MineEntity> implements MainActivityContract.View {


    @Override
    protected void reloadActivity() {

    }

    @Override
    protected View injectTarget() {
        return null;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    public int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    public void showDataSuccess(MineEntity datas) {

    }


    @Override
    public void showSuccess(MineEntity mineEntity) {

    }
}
