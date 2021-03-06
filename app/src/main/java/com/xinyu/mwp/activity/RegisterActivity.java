package com.xinyu.mwp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import com.xinyu.mwp.R;
import com.xinyu.mwp.activity.base.BaseControllerActivity;
import com.xinyu.mwp.application.MyApplication;
import com.xinyu.mwp.entity.LoginReturnEntity;
import com.xinyu.mwp.entity.RegisterReturnEntity;
import com.xinyu.mwp.entity.RegisterVerifyCodeEntry;
import com.xinyu.mwp.entity.UserEntity;
import com.xinyu.mwp.entity.WXUserInfoEntity;
import com.xinyu.mwp.exception.CheckException;
import com.xinyu.mwp.helper.CheckHelper;
import com.xinyu.mwp.listener.OnAPIListener;
import com.xinyu.mwp.networkapi.NetworkAPIFactoryImpl;
import com.xinyu.mwp.networkapi.socketapi.SocketReqeust.SocketAPINettyBootstrap;
import com.xinyu.mwp.user.UserManager;
import com.xinyu.mwp.util.ErrorCodeUtil;
import com.xinyu.mwp.util.LogUtil;
import com.xinyu.mwp.util.MD5Util;
import com.xinyu.mwp.util.SHA256Util;
import com.xinyu.mwp.util.ToastUtils;
import com.xinyu.mwp.util.Utils;
import com.xinyu.mwp.util.VerifyCodeUtils;
import com.xinyu.mwp.view.WPEditText;

import org.xutils.view.annotation.ViewInject;

/**
 * @author : created by chuangWu
 * @version : 0.01
 * @email : chuangwu127@gmail.com
 * @created time : 2017-01-11 11:30
 * @description : none
 * @for your attention : none
 * @revise : none
 */
public class RegisterActivity extends BaseControllerActivity {
    @ViewInject(R.id.phoneEditText)
    private WPEditText phoneEditText;
    @ViewInject(R.id.msgEditText)
    private WPEditText msgEditText;
    @ViewInject(R.id.pwdEditText)
    private WPEditText pwdEditText;
    @ViewInject(R.id.wpe_member_unit)
    private WPEditText memberUnit;
    @ViewInject(R.id.wpe_agent_id)
    private WPEditText agentId;
    @ViewInject(R.id.wpe_referee_id)
    private WPEditText refereeId;
    @ViewInject(R.id.nextButton)
    private Button nextButton;
    private CheckHelper checkHelper = new CheckHelper();
    private RegisterReturnEntity registerEntity;
    private String phone;
    private String pwd;
    private String vCode;
    private boolean isBind = false;
    private String newPwd;
    private long memberUnitText;
    private String agentIdText;
    private String refereeIdText;
    private WXUserInfoEntity entity;

    @Override
    protected int getContentView() {
        return R.layout.activity_register;
    }

    @Override
    protected void initView() {
        super.initView();
        String title = "注册";
        Bundle bundle = getIntent().getBundleExtra("wx");
        if (bundle != null) {
            entity = (WXUserInfoEntity) bundle.getSerializable("wxBind");
            title = "请绑定手机号码";
            isBind = true;
        }

        setTitle(title);
        phoneEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        checkHelper.checkButtonState(nextButton, phoneEditText, msgEditText, pwdEditText);
        checkHelper.checkVerificationCode(msgEditText.getRightText(), phoneEditText);
        memberUnit.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    protected void initListener() {
        super.initListener();

        msgEditText.getRightText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!SocketAPINettyBootstrap.getInstance().isOpen()) {
                    ToastUtils.show(context, "网络连接失败,请检查网络");
                    return;
                }
                int verifyType = 0;// 0-注册 1-登录 2-更新服务
                VerifyCodeUtils.getCode(msgEditText, verifyType, context, view, phoneEditText);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String loader = "正在注册...";
                if (isBind) {
                    loader = "正在绑定...";
                }
                showLoader(loader);
                CheckException exception = new CheckException();
                phone = phoneEditText.getEditTextString();
                pwd = pwdEditText.getEditTextString();
                vCode = msgEditText.getEditTextString();

                if (checkHelper.checkMobile(phone, exception) && checkHelper.checkPassword(pwd, exception)
                        && checkHelper.checkVerifyCode(vCode, exception)) {
                    Utils.closeSoftKeyboard(v);

                    newPwd = SHA256Util.shaEncrypt(SHA256Util.shaEncrypt(pwd + "t1@s#df!") + phone);
                    memberUnitText = 0;
                    if (!TextUtils.isEmpty(memberUnit.getEditTextString())) {
                        memberUnitText = Long.parseLong(memberUnit.getEditTextString());
                    }
                    agentIdText = agentId.getEditTextString();
                    refereeIdText = refereeId.getEditTextString();
                    if (isBind) {
                        bindUserInfo();
                    } else {
                        register();
                    }
                } else {
                    closeLoader();
                    showToast(exception.getErrorMsg());
                }
            }
        });
    }

    /**
     * 微信登录--绑定用户信息
     */
    private void bindUserInfo() {
        NetworkAPIFactoryImpl.getUserAPI().bindNumber(phone, entity.getOpenid(), newPwd, vCode, memberUnitText,
                agentIdText, refereeIdText, entity.getNickname(), entity.getHeadimgurl(), new OnAPIListener<RegisterReturnEntity>() {
                    @Override
                    public void onError(Throwable ex) {
                        ex.printStackTrace();
                        LogUtil.d("绑定失败!");
                        closeLoader();
                        ErrorCodeUtil.showEeorMsg(context, ex);
                    }

                    @Override
                    public void onSuccess(RegisterReturnEntity registerReturnEntity) {
                        LogUtil.d("绑定账号成功!" + registerReturnEntity.toString());
                        loginGetUserInfo(newPwd);  //调用登录
                    }
                });
    }

    private void register() {
        //本地校验验证码   MD5(yd1742653sd + code_time + rand_code + phone)
        if (!RegisterVerifyCodeEntry.vToken.equals(MD5Util.MD5("yd1742653sd" + RegisterVerifyCodeEntry.timeStamp + vCode))) {
           ToastUtils.show(context,"验证码错误,请重新输入");
            closeLoader();
            return;
        }

        NetworkAPIFactoryImpl.getUserAPI().register(phone, newPwd, vCode, memberUnitText, agentIdText, refereeIdText,
                new OnAPIListener<RegisterReturnEntity>() {
                    @Override
                    public void onError(Throwable ex) {
                        ex.printStackTrace();
                        closeLoader();
                        ErrorCodeUtil.showEeorMsg(context, ex);
                    }

                    @Override
                    public void onSuccess(RegisterReturnEntity registerReturnEntity) {
                        registerEntity = registerReturnEntity;
                        LogUtil.d("注册请求网络成功" + registerEntity.toString());
                        closeLoader();
                        if (registerEntity.result == 0) {
                            ToastUtils.show(context, "用户已经注册,请直接登录");
                            next(LoginActivity.class);
                            finish();
                        } else if (registerEntity.result == 1) {
                            ToastUtils.show(context, "注册成功");
//                            loginGetUserInfo(newPwd);  //登录请求数据
                            finish();
                        }
                    }
                });
    }

    /**
     * 登录获取用户信息
     *
     * @param newPwd 加密后的pwd
     */
    private void loginGetUserInfo(String newPwd) {
        NetworkAPIFactoryImpl.getUserAPI().login(phone, newPwd, null,
                new OnAPIListener<LoginReturnEntity>() {
                    @Override
                    public void onError(Throwable ex) {
                        ex.printStackTrace();
                    }

                    @Override
                    public void onSuccess(LoginReturnEntity loginReturnEntity) {
                        UserEntity en = new UserEntity();
                        en.setBalance(loginReturnEntity.getUserinfo().getBalance());
                        en.setId(loginReturnEntity.getUserinfo().getId());
                        en.setToken(loginReturnEntity.getToken());
                        en.setUserType(loginReturnEntity.getUserinfo().getType());
                        en.setMobile(loginReturnEntity.getUserinfo().getPhone());
                        UserManager.getInstance().saveUserEntity(en);
                        UserManager.getInstance().setLogin(true);
                        MyApplication.getApplication().onUserUpdate(true);
//                        finish();
                        LogUtil.d("调用登录成功了");
                        //绑定成功,登录成功--发送消息,进入首页
                        // EventBus.getDefault().postSticky(new EventBusMessage(-7));  //传递消息
                        Intent intent = new Intent(RegisterActivity.this, MainFragmentActivity.class);
                        startActivity(intent);
                    }
                });
    }
}

