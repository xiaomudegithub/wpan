package com.xinyu.mwp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xinyu.mwp.R;
import com.xinyu.mwp.adapter.base.BaseListViewAdapter;
import com.xinyu.mwp.adapter.viewholder.BaseViewHolder;
import com.xinyu.mwp.entity.BankCardEntity;
import com.xinyu.mwp.listener.OnAPIListener;
import com.xinyu.mwp.networkapi.NetworkAPIFactoryImpl;
import com.xinyu.mwp.util.BankInfoUtil;
import com.xinyu.mwp.util.DisplayUtil;
import com.xinyu.mwp.util.ErrorCodeUtil;
import com.xinyu.mwp.util.GradientDrawableUtil;
import com.xinyu.mwp.util.LogUtil;
import com.xinyu.mwp.util.NumberUtils;
import com.xinyu.mwp.util.ToastUtils;
import com.xinyu.mwp.view.SwipeListLayout;

import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

import in.srain.cube.views.ptr.PtrFrameLayout;

/**
 * @author : Created by xiepeng
 * @email : xiepeng2015929@gmail.com
 * @created time : 2017/1/16 0016
 * @describe : com.xinyu.mwp.adapter
 */
public class BindBankCardAdapter extends BaseListViewAdapter<BankCardEntity> {

    private PtrFrameLayout ptrFrameLayout;

    public BindBankCardAdapter(Context context) {
        super(context);
    }

    public void setPtrFrameLayout(PtrFrameLayout ptrFrameLayout) {
        this.ptrFrameLayout = ptrFrameLayout;
    }

    @Override
    protected BaseViewHolder<BankCardEntity> getViewHolder(int position) {
        return new BindBankCardViewHolder(context);
    }

    class BindBankCardViewHolder extends BaseViewHolder<BankCardEntity> {

        @ViewInject(R.id.swipeLayout)
        private SwipeListLayout swipeLayout;
        @ViewInject(R.id.itemLayout)
        private LinearLayout itemLayout;
        @ViewInject(R.id.iconView)
        private ImageView iconView;
        @ViewInject(R.id.titleView)
        private TextView titleView;
        @ViewInject(R.id.typeView)
        private TextView typeView;
        @ViewInject(R.id.numberView)
        private TextView numberView;
        @ViewInject(R.id.deleteView)
        private TextView deleteView;
        private BankCardEntity bankCardEntity;

        public BindBankCardViewHolder(Context context) {
            super(context);
        }

        @Override
        protected int layoutId() {
            return R.layout.item_bind_bank_card2;
        }

//        @Override
//        protected void initView() {
//            super.initView();
//            if (ptrFrameLayout != null) {
//                swipeLayout.setPtrFrameLayout(ptrFrameLayout);
//            }
//        }

        //存放所有已经打开的菜单
        private List<SwipeListLayout> openList = new ArrayList<SwipeListLayout>();

        @Override
        protected void initListener() {
            super.initListener();
            deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    unBindcard(); //解除绑定的银行卡
                }
            });
            swipeLayout.setSwipeChangeListener(new SwipeListLayout.OnSwipeChangeListener() {
                @Override
                public void onStartOpen(SwipeListLayout mSwipeLayout) {
                    for (SwipeListLayout layout : openList) {
                        layout.close();
                    }
                    openList.clear();
                }

                @Override
                public void onStartClose(SwipeListLayout mSwipeLayout) {
                }

                @Override
                public void onOpen(SwipeListLayout mSwipeLayout) {
                    openList.add(mSwipeLayout);
                }

                @Override
                public void onDraging(SwipeListLayout mSwipeLayout) {
                }

                @Override
                public void onClose(SwipeListLayout mSwipeLayout) {
                    openList.remove(mSwipeLayout);
                }
            });
        }

        /**
         * 解除绑定银行卡
         */
        private void unBindcard() {
            NetworkAPIFactoryImpl.getDealAPI().unBindCard(bankCardEntity.getBid(), "", new OnAPIListener<Object>() {
                @Override
                public void onError(Throwable ex) {
                    ex.printStackTrace();
                    ErrorCodeUtil.showEeorMsg(context, ex);
                }

                @Override
                public void onSuccess(Object o) {
                    remove(position);
                    notifyDataSetChanged();
                    swipeLayout.close();
                }
            });
        }

        @Event(value = R.id.rl_bank)
        private void click(View v) {
            onItemChildViewClick(v, 99);
        }

        @Override
        protected void update(BankCardEntity data) {
            if (data != null) {
                bankCardEntity = data;
//                ImageLoader.getInstance().displayImage(data.getIcon(), iconView);
                iconView.setImageResource(BankInfoUtil.getIcon(data.getBank()));
                titleView.setText(data.getBank());
                typeView.setText("储蓄卡");
                String cardEnd4 = NumberUtils.formatCard(data.getCardNo());
                numberView.setText(cardEnd4);
                Drawable drawable = GradientDrawableUtil.getGradientDrawable(Color.parseColor(data.getBackGround()), DisplayUtil.dip2px(5, context));
                itemLayout.setBackgroundDrawable(drawable);
                String menuBg = "#";
                if (data.getBackGround().contains("#")) {
                    menuBg = data.getBackGround().substring(1);
                    menuBg = "#87" + menuBg;
                }
                Drawable menuDrawable = GradientDrawableUtil.getGradientDrawable(Color.parseColor(menuBg), DisplayUtil.dip2px(5, context));
                deleteView.setBackgroundDrawable(menuDrawable);
            }
        }
    }
}
