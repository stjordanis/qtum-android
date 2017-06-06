package com.pixelplex.qtum.ui.fragment.TokenFragment;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.pixelplex.qtum.R;
import com.pixelplex.qtum.dataprovider.RestAPI.gsonmodels.ContractInfo;
import com.pixelplex.qtum.ui.fragment.BaseFragment.BaseFragment;
import com.pixelplex.qtum.ui.fragment.BaseFragment.BaseFragmentPresenterImpl;
import com.pixelplex.qtum.utils.FontTextView;
import com.pixelplex.qtum.utils.StackCollapseLinearLayout;
import com.transitionseverywhere.ChangeClipBounds;
import com.transitionseverywhere.TransitionManager;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by kirillvolkov on 01.06.17.
 */

public class TokenFragment extends BaseFragment implements TokenFragmentView {

    public final int LAYOUT = R.layout.lyt_token_fragment;
    public static final String tokenKey = "tokenInfo";

    public static final String totalSupply = "totalSupply";
    public static final String decimals = "decimals";

    public static TokenFragment newInstance(ContractInfo token) {
        Bundle args = new Bundle();
        TokenFragment fragment = new TokenFragment();
        args.putSerializable(tokenKey,token);
        fragment.setArguments(args);
        return fragment;
    }

    TokenFragmentPresenter presenter;

    @OnClick(R.id.bt_back)
    public void onBackClick(){
        getActivity().onBackPressed();
    }

    //HEADER
    @BindView(R.id.tv_balance)
    FontTextView balanceValue;
    @BindView(R.id.available_balance_title)
    FontTextView balanceTitle;

    @BindView(R.id.tv_unconfirmed_balance)
    FontTextView uncomfirmedBalanceValue;
    @BindView(R.id.unconfirmed_balance_title)
    FontTextView uncomfirmedBalanceTitle;
    //HEADER

    @BindView(R.id.balance_view)
    FrameLayout balanceView;

    @BindView(R.id.fade_divider)
    View fadeDivider;

    @BindView(R.id.fade_divider_root)
    RelativeLayout fadeDividerRoot;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.app_bar)
    AppBarLayout mAppBarLayout;

    ChangeClipBounds clip;

    @BindView(R.id.collapse_layout)
    StackCollapseLinearLayout collapseLinearLayout;

    @BindView(R.id.tv_token_address)
    FontTextView tokenAddress;

    @BindView(R.id.contract_address_value)
    FontTextView contractAddress;

    @BindView(R.id.initial_supply_value)
    FontTextView totalSupplyValue;

    @BindView(R.id.decimal_units_value)
    FontTextView decimalsValue;

    @BindView(R.id.sender_address_value)
    FontTextView senderAddrValue;

    @Override
    protected void createPresenter() {
        presenter = new TokenFragmentPresenter(this);
    }

    @Override
    protected BaseFragmentPresenterImpl getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayout() {
        return LAYOUT;
    }

    @Override
    public void initializeViews() {
        super.initializeViews();

        presenter.setToken((ContractInfo) getArguments().getSerializable(tokenKey));
        presenter.getPropertyValue(totalSupply);
        presenter.getPropertyValue(decimals);

        collapseLinearLayout.requestLayout();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        clip = new ChangeClipBounds();
        clip.addTarget(fadeDivider);
        clip.setDuration(300);

        uncomfirmedBalanceValue.setVisibility(View.GONE);
        uncomfirmedBalanceTitle.setVisibility(View.GONE);

        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(),R.color.colorAccent));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.onRefresh();
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (!mSwipeRefreshLayout.isActivated()) {
                    if (verticalOffset == 0) {
                        mSwipeRefreshLayout.setEnabled(true);
                    } else {
                        mSwipeRefreshLayout.setEnabled(false);
                    }
                }

                float percents = (((getTotalRange() - Math.abs(verticalOffset))*1.0f)/getTotalRange());

                balanceView.setAlpha((percents>0.5f)? percents : 1 - percents);

                if(percents == 0){
                    doDividerExpand();
                } else {
                    doDividerCollapse();
                }

                animateText(percents, balanceTitle);
                balanceTitle.setX(appBarLayout.getWidth() / 2 * percents - (balanceTitle.getWidth() * percents) / 2);
                balanceTitle.setY(balanceView.getHeight()/2 + balanceTitle.getHeight()/2 * percents - balanceTitle.getHeight()/2 * (1-percents));

                animateText(percents, balanceValue);
                balanceValue.setX(appBarLayout.getWidth() - (appBarLayout.getWidth() / 2 * percents + (balanceValue.getWidth() * percents) / 2) - balanceValue.getWidth() * (1 - percents));
                balanceValue.setY(balanceView.getHeight() / 2 - balanceValue.getHeight() * percents - balanceValue.getHeight()/2 * (1-percents));
                collapseLinearLayout.collapseFromPercents(percents);
            }
        });
    }

    public void doDividerExpand() {
        fadeDivider.setVisibility(View.VISIBLE);
        TransitionManager.endTransitions(fadeDividerRoot);
        fadeDivider.setClipBounds(new Rect(0,0,0,fadeDivider.getHeight()));
        TransitionManager.beginDelayedTransition(fadeDividerRoot, clip);
        fadeDivider.setClipBounds(new Rect(0,0,getResources().getDisplayMetrics().widthPixels,fadeDivider.getHeight()));
    }

    public void doDividerCollapse() {
        fadeDivider.setVisibility(View.INVISIBLE);
    }

    public int getTotalRange() {
        return mAppBarLayout.getTotalScrollRange();
    }

    private void animateText(float percents, View view) {
        if(percents>0.9f) {
            view.setScaleX(percents);
            view.setScaleY(percents);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void setBalance(float balance) {
        balanceValue.setText(String.format("%f QTUM",balance));
    }

    @Override
    public void setTokenAddress(String address) {
        if(!TextUtils.isEmpty(address)) {
            tokenAddress.setText(address);
            contractAddress.setText(address);
        }
    }

    @Override
    public void onContractPropertyUpdated(String propName, String propValue) {
        switch (propName){
            case totalSupply:
                totalSupplyValue.setText(propValue);
                break;
            case decimals:
                decimalsValue.setText(propValue);
                break;
        }
    }

    @Override
    public void setSenderAddress(String address) {
        if(!TextUtils.isEmpty(address)) {
            senderAddrValue.setText(address);
        }
    }
}