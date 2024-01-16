package com.thinksoftware.nora.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.api.internal.GoogleApiManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.thinksoftware.nora.MainActivity;
import com.thinksoftware.nora.R;
import com.thinksoftware.nora.adapters.PedidoAdapter;
import com.thinksoftware.nora.bases.SuperClass;
import com.thinksoftware.nora.dbe.MyDatabaseHelper;
import com.thinksoftware.nora.models.ListaFirebase;
import com.thinksoftware.nora.models.Pedido;
import com.thinksoftware.nora.models.User;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static com.thinksoftware.nora.bases.SuperClass.isLogin;
import static com.thinksoftware.nora.bases.SuperClass.isMapa;
import static com.thinksoftware.nora.bases.SuperClass.isTransporte;
import static com.thinksoftware.nora.bases.SuperClass.locTipoRequest;
import static com.thinksoftware.nora.bases.SuperClass.xAdress;
import static com.thinksoftware.nora.bases.SuperClass.xId;
import static com.thinksoftware.nora.bases.SuperClass.xName;
import static com.thinksoftware.nora.bases.SuperClass.xPhone;
import static com.thinksoftware.nora.bases.SuperClass.xToken;
import static com.thinksoftware.nora.bases.SuperClass.xtotalPedido;

public class PedidoFragment extends Fragment implements PedidoAdapter.EventListener {

    public PedidoFragment(){}
    String TAG = "HGG";
    private Context mContext;
    private SuperClass sc;
    private PedidoAdapter adapter;
    private List<Pedido> itemList;
    private MyDatabaseHelper myDB;
    public TextView transporte,transporte_text,total_pedido,total_text,subtotal;
    public TextView adre_use,pro_txt,costtotal;
    private ScrollView scrollView;
    private FloatingActionButton fabP;
    boolean editado_r = false; // editado recipiente
    private EditText recip,nr_fijo,nr_celular,adre_ref,adre_not;
    public EditText nr_direccion;
    private ImageView gpsNoRegistro,verify,transporteInfo;
    public ImageView splash;
    protected GoogleApiManager mGoogleApiClient;
    private Button procesarPedido,edit_recip;
    private RelativeLayout rlContainer;
    private Button btnVerificar,editVerificado,enviar;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    EditText codiEdit;
    private Dialog dialogSMS,dialogDone;
    FirebaseFirestore db;
    LinearLayout ll,lls;
    public LinearLayout proLl; // progress bar y calculando
    ProgressBar progress;
    public ProgressBar transProgress;
    public RelativeLayout rlNoGps;
    public ProgressDialog mAuthProgressDialog;
    private View fpview;
    Dialog progressDialog;
    String tel;

    
    public static PedidoFragment newInstance(){
        return new PedidoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_pedido, container, false);
        mContext = getActivity();
        /*requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);*/
        initializeValues(view);
        myDB = new MyDatabaseHelper(mContext);
        RecyclerView recyclerView = view.findViewById(R.id.pe_rv);
        scrollView = view.findViewById(R.id.pe_scroll);
        fabP = view.findViewById(R.id.pe_fab);
        itemList = new ArrayList<>();
        adapter = new PedidoAdapter(mContext,itemList,this);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new SuperClass.GridSpacingItemDecoration(2, sc.dpToPx(4), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(true);
        prepareItems("init","in");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollView.setOnScrollChangeListener((view15, i, i1, i2, i3) -> {
                view15 = scrollView.getChildAt(scrollView.getChildCount() - 1);
                int diff = (view15.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
                // if diff is zero, then the bottom has been reached
                if (diff > 0) {
                    fabP.show();
                }else fabP.hide();
            });
        }

        scrollView.getViewTreeObserver()
                .addOnScrollChangedListener(() -> {
                    if (scrollView.getChildAt(0).getBottom()
                            <= (scrollView.getHeight() + scrollView.getScrollY())) {
                        fabP.hide();
                    }
                });

        fabP.setOnClickListener(view1 ->scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        editVerificado.setOnClickListener(view14 -> {
            //TODO programar el cambio de telefono
            Snackbar.make(rlContainer, R.string.esta_seguro,
                    Snackbar.LENGTH_SHORT)
                    .show();
            phoneNoVerificado();
        });

        edit_recip.setOnClickListener(view1 -> edit_recipiente());
        gpsNoRegistro.setOnClickListener(view2 -> getGpsConnection());
        procesarPedido.setOnClickListener(view12 -> procesar());
        btnVerificar.setOnClickListener(view13 -> smsDialog());
        /*if (view.getRootView() != null){
            //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            view.getRootView().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
           //view.getRootView().setBackgroundColor(Color.TRANSPARENT);
            Log.d(TAG,"aqui se aprieta getview");
        }*/
        //v.getRootView().setBackgroundColor(Color.TRANSPARENT);

/*
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull @NotNull PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential.getSmsCode());
                codiEdit.setText(credential.getSmsCode());
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull @NotNull FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            @Override
            public void onCodeSent(@NonNull @NotNull String verificationId, @NonNull @NotNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };*/
       return view;
    }

    private void verifyCode(String codeByUser) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, codeByUser);
        signInWithPhoneAuthCredential(credential);
    }

    private void smsDialog() {
        tel = nr_celular.getText().toString();
        if (isTelefonoValid(tel)) {
            fpview.setVisibility(View.VISIBLE);
            dialogSMS = new Dialog(mContext, R.style.MyCustomTheme);
            dialogSMS.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogSMS.setCancelable(false);
            dialogSMS.setContentView(R.layout.dialogsms);
            dialogSMS.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            TextView info, tit;
            progress = dialogSMS.findViewById(R.id.sms_progress);
            tit = dialogSMS.findViewById(R.id.sms_titulo);
            info = dialogSMS.findViewById(R.id.sms_desc);
            tit.setText(getString(R.string.sms_txt_desc));
            enviar = dialogSMS.findViewById(R.id.sms_ok);
            Button logout = dialogSMS.findViewById(R.id.sms_logout);
            TextView num = dialogSMS.findViewById(R.id.sms_numero);
            TextView editar = dialogSMS.findViewById(R.id.sms_btn_editar);
            TextView ok = dialogSMS.findViewById(R.id.sms_btn_ok);
            TextView preg = dialogSMS.findViewById(R.id.sms_preg);
            ImageView cerrar = dialogSMS.findViewById(R.id.sms_close);
            codiEdit = dialogSMS.findViewById(R.id.sms_edit);
            if (tel.charAt(0) != '+' && tel.length() == 8) tel = "+591"+tel;
            num.setText(tel);
            ok.setOnClickListener(v -> {
                ok.setVisibility(View.GONE);
                editar.setVisibility(View.GONE);
                preg.setVisibility(View.GONE);
                enviar.setVisibility(View.VISIBLE);
                info.setVisibility(View.VISIBLE);
            });
            editar.setOnClickListener(view -> {
                dialogSMS.dismiss();
                nr_celular.requestFocus();
                fpview.setVisibility(View.GONE);
            });
            enviar.setOnClickListener(view -> {
                if (enviar.getText().equals("ENVIAR SMS")) {
                    testPhoneVerify(tel);
                    enviar.animate().setDuration(1500).alpha(0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progress.setVisibility(View.VISIBLE);
                            progress.setAlpha(0);
                            enviar.setAlpha(0);
                            progress.animate().setDuration(1500).alpha(1).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    progress.setVisibility(View.VISIBLE);
                                    codiEdit.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                }else{
                    showProgress();
                    String code = codiEdit.getText().toString();
                    if (codeisValid(code)) verifyCode(code);
                    fpview.setVisibility(View.GONE);
                }
            });
            cerrar.setOnClickListener(view -> {
                ok.setVisibility(View.VISIBLE);
                editar.setVisibility(View.VISIBLE);
                preg.setVisibility(View.VISIBLE);
                enviar.setVisibility(View.GONE);
                info.setVisibility(View.GONE);
                codiEdit.setVisibility(View.GONE);
                btnVerificar.setVisibility(View.VISIBLE);
                verify.setVisibility(View.GONE);
                dialogSMS.dismiss();
                fpview.setVisibility(View.GONE);
            });
            logout.setOnClickListener(view ->FirebaseAuth.getInstance().signOut());
            dialogSMS.show();
        }
    }

    private boolean codeisValid(String code) {
        if (code.length() != 6) {
            codiEdit.setError("El número debe tener al menos 6 digitos");
            codiEdit.requestFocus();
            progressDialog.dismiss();
            return false;
        }
        return true;
    }

    public void testPhoneVerify(String phoneNum) {
        // [START auth_test_phone_verify]
        //String phoneNum = "+16505554567";

        // Whenever verification is triggered with the whitelisted number,
        // provided it is not set for auto-retrieval, onCodeSent will be triggered.

        FirebaseAuth auth = FirebaseAuth.getInstance();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNum)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onCodeSent(@NotNull String verificationId,
                                           @NotNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        mVerificationId = verificationId;
                        progress.animate().setDuration(1500).alpha(0).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                enviar.setVisibility(View.VISIBLE);
                                enviar.setText(getString(R.string.dialog_validar));
                                enviar.setAlpha(0);
                                progress.setAlpha(0);
                                enviar.animate().setDuration(1500).alpha(1).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        enviar.setVisibility(View.VISIBLE);
                                        codiEdit.setEnabled(true);
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onVerificationCompleted(@NotNull PhoneAuthCredential phoneAuthCredential) {
                        // Sign in with the credential
                        // ...
                        if (!Objects.requireNonNull(phoneAuthCredential.getSmsCode()).isEmpty())
                        codiEdit.setText(phoneAuthCredential.getSmsCode());
                    }

                    @Override
                    public void onVerificationFailed(@NotNull FirebaseException e) {

                    }                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        // [END auth_test_phone_verify]
    }

    // Procesar el pedido
    private void procesar() {
        showProgress();
        procesarPedido.setEnabled(false);
        if (isTransporte) {
            String direc = nr_direccion.getText().toString();
            String celular = nr_celular.getText().toString();
            double tot = Double.parseDouble(total_pedido.getText().toString());
            if (!isTelefonoValid(celular) || !isDireccionValid(direc)) {
                Snackbar.make(rlContainer, R.string.error_registro,
                        Snackbar.LENGTH_SHORT)
                        .show();
                procesarPedido.setEnabled(true);
                progressDialog.dismiss();
            } else if (!sc.ifInternet()) {
                ((MainActivity) mContext).showDiagEjecute("Necesitas Conectarte a Internet para continuar", 2, "internet");
                procesarPedido.setEnabled(true);
                progressDialog.dismiss();
            } else {
                final boolean[] isOnline = new boolean[1];
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    try {
                        Runtime runtime = Runtime.getRuntime();
                        Process p = runtime.exec("ping -c 1 8.8.8.8");
                        int waitFor = p.waitFor();
                        isOnline[0] = waitFor == 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(() -> {
                        if (isOnline[0]) {
                            if (!sc.isGPSgot) {
                                Cursor x = myDB.searchPedidosAnteriores();
                                if (x.getCount()>0&&!isMapa){
                                    fpview.setVisibility(View.VISIBLE);
                                    x.moveToLast();
                                    sc.xLatitud = Double.parseDouble(x.getString(12));
                                    sc.xLongitud = Double.parseDouble(x.getString(13));
                                    progressDialog.dismiss();
                                    dialognoGPS(celular);
                                }else {
                                    if (isMapa){
                                        fpview.setVisibility(View.VISIBLE);
                                        progressDialog.dismiss();
                                        dialogPreConfirm(tot, direc, celular,"","","");
                                        //continuarProcesar(direc, celular,"","","");
                                    }
                                    else{
                                        procesarPedido.setEnabled(true);
                                        progressDialog.dismiss();
                                        ((MainActivity) mContext).showDiagEjecute("No pudimos recuperar tu localización, por favor señala tu ubicación en el mapa", 3, "mapa");
                                    }
                                }
                                x.close();
                            } else {
                                fpview.setVisibility(View.VISIBLE);
                                progressDialog.dismiss();
                                dialogPreConfirm(tot, direc, celular,"","","");
                            }
                        } else {
                            ((MainActivity) mContext).showDiagEjecute("Al parecer tu conexión es un poco inestable, intenta nuevamente", 2, "internet");
                            procesarPedido.setEnabled(true);
                            progressDialog.dismiss();
                        }
                    });
                });
                executor.shutdown();
            }
        }else{
            procesarPedido.setEnabled(true);
            Snackbar.make(rlContainer, R.string.str_calc_trans,
                    Snackbar.LENGTH_SHORT)
                    .show();
            progressDialog.dismiss();
        }
    }

    private void continuarProcesar(String direc, String celular, String tr, String to, String comeFrom,boolean deltime) {
        String entre = recip.getText().toString();
        xName = entre;
        String fecha = sc.fecha("fecha");
        String hora = sc.fecha("hora");
        String fijo = nr_fijo.getText().toString();
        String refer = adre_ref.getText().toString();
        String items = extractPedido();
        String subt,trans,total;
        String lat = String.valueOf(sc.xLatitud);
        String lon = String.valueOf(sc.xLongitud);
        String deliverytime;
        if (deltime) deliverytime="inmediata"; else deliverytime="programada";
        if (comeFrom.equals("nogps")){
            subt = String.valueOf(xtotalPedido);
            trans = tr;
            total = to;
            Cursor x = myDB.searchLastLocation();
            myDB.addDirecciones(direc,lat,lon,fecha, x.getCount() > 20,x.getCount());
            x.close();
        }else {
            subt = subtotal.getText().toString();
            trans = transporte.getText().toString();
            total = total_pedido.getText().toString();
        }
        String verificado;
        if (isLogin) verificado = "si";
        else verificado = "no";
        //TODO VERIFICAR LOCATION SI O SI
        String not = adre_not.getText().toString();
        myDB.addPedidoConfirmado("",fecha, hora, entre, items, subt, trans, total, fijo, celular, "", lat, lon, direc, refer, not,"no","");
        ListaFirebase lFirebase = new ListaFirebase(fecha, hora, entre, items, subt, trans, total, fijo, celular, verificado, lat, lon, direc, refer, not,"noSU PEDIDO ESTÁ SIENDO PREPARADO",deliverytime,"no","no","");
        savePedidoFirebase(lFirebase, subt, celular, entre,deltime);
    }

    private void savePedidoFirebase(ListaFirebase lFirebase,String total,String cel, String entre,boolean delt) {
        db.collection("pedidos")
                .add(lFirebase)
                .addOnSuccessListener(unused -> {
                    String x = unused.getId();
                    Cursor xx = myDB.selectPedidosMuid();
                    String id = xx.getString(0);
                    myDB.updatePedidosMuid(id,x);
                    updateUInoPedido(total, delt);
                });
        if (isLogin){
            db.collection("users").document(xPhone).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    DocumentSnapshot dc = task.getResult();
                    if (dc.exists()){
                        String g = dc.getString("total");
                        xName = dc.getString("name");
                        String Grand;
                        if (TextUtils.isEmpty(g)) Grand = total;
                        else{
                            assert g != null;
                            double max = Double.parseDouble(g)+Double.parseDouble(total);
                            Grand = String.valueOf(max);
                        }
                        db.collection("users")
                                .document(xPhone).update("total",Grand);
                    }
                }
            });
        }else{
            getTokenFirebaseMessaging(cel,entre,cel,"no",total); // notLogin
        }
    }

    private void updateUInoPedido(String total, boolean delt) {
        String pednum = myDB.closePedido(total);
        myDB.updatepedcontable(pednum);
        eraseBadge();
        closeFragment();
        mAuthProgressDialog.dismiss();
        if (delt) ((MainActivity)mContext).showDiagEjecute(getString(R.string.dialog_success_pedido),1,"closePedido");
        else ((MainActivity)mContext).showDiagEjecute(getString(R.string.dialog_success_pedido),1,"closePedidoPer");
        isMapa=false;
        if (progressDialog.isShowing()) progressDialog.dismiss();
    }

    @NotNull
    private String extractPedido() {
        StringBuilder result = new StringBuilder();
        Cursor lpCursor = myDB.searchPedido("open");
        if (lpCursor.getCount()>0){
            lpCursor.moveToFirst();
            String pedidoActual = lpCursor.getString(2);
            Cursor c = myDB.searchListPedidos(pedidoActual);
            if (c.getCount()>0){
                c.moveToFirst();
                for (int i=0;i<c.getCount();i++){
                    result.append("|");
                    result.append(c.getString(5));result.append("|");
                    result.append(c.getString(4));result.append("|");
                    result.append(c.getString(6));result.append("|");
                    c.moveToNext();
                }
            }
        }
        lpCursor.close();
        return result.toString();
    }

    private boolean isTelefonoValid(String telefono) {
        if (telefono.length() < 8) {
            nr_celular.setError("El número debe ser un número valido");
            nr_celular.requestFocus();
            return false;
        }
        return true;
    }


    private boolean isDireccionValid(String direccion) {
        if (direccion.matches("")) {
            nr_direccion.setError("Debes introducir una dirección valida");
            nr_direccion.requestFocus();
            return false;
        }else return true;
    }

    private void checkifLocation() {
        locTipoRequest = "pedido";
        //Log.d(TAG,"checkiflocation");
        ((MainActivity)mContext).runPermission(locTipoRequest);
       /* if (!sc.isGPSgot){
            rlNoGps.setVisibility(View.VISIBLE); // (I)logo y esperando ubicacion
            locTipoRequest = "pedido";
            ((MainActivity)mContext).runPermission(locTipoRequest);
        }else{
            proLl.setVisibility(View.VISIBLE); // Cargando Transporte
            calculateTransporte(xtotalPedido);
        }*/
    }



    private void getGpsConnection() {
        nr_direccion.setError(null);
        /*Intent i = new Intent(requireContext().getApplicationContext(), PedidoFragment.class);
        if (sc.isGPSgot){
            i.putExtra("lat", sc.xLatitud);
            i.putExtra("lon", sc.xLongitud);
        }
        startActivity(i);*/
        if (!sc.isGPSgot){
            locTipoRequest="mapa";
            ((MainActivity)mContext).runPermission("mapa");
        }else{
            MapsFragment nextFrag= new MapsFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                   //.replace(R.id.frag_container, nextFrag, "mapsFragment")
                    .add(R.id.frag_container,nextFrag,"mapsFragment")
                    .addToBackStack("mapsFragment")
                    .commit();
        }
    }

    private void initializeValues(View v) {
        TextView peTitu = v.findViewById(R.id.pe_titulo);
        TextView peSubTit = v.findViewById(R.id.pe_txt_tit);
        mAuthProgressDialog = new ProgressDialog(mContext);
        fpview = v.findViewById(R.id.fp_view);
        mAuthProgressDialog.setTitle(getString(R.string.progress_dialog_loading));
        mAuthProgressDialog.setMessage(getString(R.string.progress_dialog_maps));
        mAuthProgressDialog.setCancelable(false);
        rlNoGps = v.findViewById(R.id.fp_rl_nogps);
        proLl = v.findViewById(R.id.pe_prog_ll); // progress bar y calculando
        pro_txt = v.findViewById(R.id.pe_pro_txt);
        splash = v.findViewById(R.id.lp_slash);
        costtotal = v.findViewById(R.id.lp_totnocost);
        ll = v.findViewById(R.id.lp_ca_telver);
        lls = v.findViewById(R.id.lp_ca_llmax);
        editVerificado = v.findViewById(R.id.lp_ver_edit);
        sc = (SuperClass) getActivity();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        btnVerificar = v.findViewById(R.id.lp_ca_Verificar);
        verify = v.findViewById(R.id.lp_ca_verificado);
        rlContainer = v.findViewById(R.id.rluno);
        procesarPedido = v.findViewById(R.id.lp_rv_btn_proceder);
        transporteInfo = v.findViewById(R.id.pe_question);// interrogacion al lado de transporte
        total_pedido = v.findViewById(R.id.pe_rv_total);
        total_text = v.findViewById(R.id.pe_rv_txt_total);
        subtotal = v.findViewById(R.id.pe_rv_subtotal);
        transporte = v.findViewById(R.id.pe_rv_transporte);
        TextView subtotal_text = v.findViewById(R.id.pe_rv_txt_subtotal);
        transporte_text = v.findViewById(R.id.pe_rv_txt_transporte);
        Typeface tf2 = Typeface.createFromAsset(v.getContext().getAssets(), "fonts/Comfortaa_Medium.ttf");
        Typeface tf3 = Typeface.createFromAsset(v.getContext().getAssets(), "fonts/Comfortaa_Bold.ttf");
        total_pedido.setTypeface(tf2);
        subtotal.setTypeface(tf2);
        transporte.setTypeface(tf2);
        pro_txt.setTypeface(tf2);
        total_text.setTypeface(tf2);
        peTitu.setTypeface(tf3);
        peSubTit.setTypeface(tf3);
        subtotal_text.setTypeface(tf2);
        transporte_text.setTypeface(tf2);
        recip = v.findViewById(R.id.lp_rv_recip);
        edit_recip = v.findViewById(R.id.lp_rv_recip_edit);
        adre_use = v.findViewById(R.id.lp_rv_adres);
        nr_direccion = v.findViewById(R.id.lp_rv_ca_direccion);// direccion cuando no registra
        nr_fijo = v.findViewById(R.id.lp_rv_ca_fijo);
        nr_celular = v.findViewById(R.id.lp_rv_ca_celular);
        adre_ref = v.findViewById(R.id.lp_rv_adre_ins);
        adre_not = v.findViewById(R.id.lp_rv_notas);
        gpsNoRegistro = v.findViewById(R.id.lp_ca_Search);
        transProgress = v.findViewById(R.id.pe_pro_tra);
        if (isLogin){
            phoneVerificado();
        }else{
            phoneNoVerificado();
        }
    }

    private void phoneNoVerificado() {
        btnVerificar.setVisibility(View.VISIBLE);
        verify.setVisibility(View.GONE);
        editVerificado.setVisibility(View.GONE);
        ll.setWeightSum(1);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,0.7f);
        nr_celular.setLayoutParams(param);
        nr_celular.setEnabled(true);
        nr_celular.requestFocus();
        float heigh = getResources().getDimension(R.dimen.chart_width);
        int he = (int) heigh;
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(0, he,0.3f);
        btnVerificar.setLayoutParams(param1);
    }

    private void phoneVerificado() {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nr_celular.setLayoutParams(param);
        btnVerificar.setVisibility(View.GONE);
        verify.setVisibility(View.VISIBLE);
        lls.setWeightSum(1);
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT,0.8f);
        ll.setLayoutParams(param1);
        editVerificado.setVisibility(View.VISIBLE);
        nr_celular.setText(xPhone);
        nr_celular.setEnabled(false);
    }

    public void prepareItems(String val, String born) {
        if (!born.equals("delete")) itemList.clear();
        xtotalPedido = 0;
        Cursor lpCursor = myDB.searchPedido("open");
        if (lpCursor.getCount()>0){
            lpCursor.moveToFirst();
            String pedidoActual = lpCursor.getString(2);
            Cursor c = myDB.searchListPedidos(pedidoActual);
            if (c.getCount()>0){
               // Log.d(TAG,"numero dfe pedidos: "+c.getCount()+" pedAct "+pedidoActual);
                c.moveToFirst();
                for (int i=0;i<c.getCount();i++){
                    if (val.equals("init")){
                    Pedido a = new Pedido(c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(0));
                    xtotalPedido+=  Double.parseDouble(c.getString(7));
                    itemList.add(a);
                    }else if (born.equals("delete")){
                        xtotalPedido+=  Double.parseDouble(c.getString(7));
                    }else{
                        Pedido a = new Pedido(c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(0));
                        xtotalPedido+=  Double.parseDouble(c.getString(7));
                        itemList.add(a);
                    }
                    c.moveToNext();
                }
                subtotal.setText(String.valueOf(xtotalPedido));
                if (!born.equals("in")&&sc.isGPSgot)calcularTransporteEdit(); // cuando no hay gps y aun no se escogio mapa
                if (born.equals("edit")&&isMapa)calcularTransporteEdit(); // cuando no hay gps y se escoge from mapa
                if (val.equals("init")&&!isMapa){
                    Cursor my = myDB.readBasicos();
                    if (my.getCount()>0){
                        my.moveToFirst();
                        sc.ceroTransporte= Integer.parseInt(my.getString(1));
                    }
                    my.close();
                    //AsyncTask.execute(this::checkifLocation);
                    checkifLocation();
                }
                if (val.equals("sum"))  sc.updateCanasta(pedidoActual,mContext);
                if (!born.equals("delete")) adapter.notifyDataSetChanged();
                if (born.equals("in")) completeDatafromPreviusPedido();
            }
            c.close();
        }
        lpCursor.close();
    }

    private void calcularTransporteEdit() {

        double total;
        if (xtotalPedido>= sc.ceroTransporte){
            String x = "0.0";
            transporte.setText(x);
            transporteInfo.setVisibility(View.VISIBLE);
            total_pedido.setText(String.valueOf(xtotalPedido));
        }else {
            if (transporte.getText().toString().equals("0.0")){
                double as= distance(sc.xLatitud, sc.xLongitud,-17.387806, -66.156655);
                String valTrans = variantTransport(as,xtotalPedido);
                transporte.setText(valTrans);
                total = Double.parseDouble(valTrans)+xtotalPedido;
                total_pedido.setText(String.valueOf(total));
            }else {
                double trans = Double.parseDouble(transporte.getText().toString());
                total = trans + xtotalPedido;
                total_pedido.setText(String.valueOf(total));
            }
        }
    }

    private void completeDatafromPreviusPedido() {
        Cursor x = myDB.searchPedidosAnteriores();
        if (x.getCount()>0){
            x.moveToLast();
            if (isLogin){
                nr_celular.setText(xPhone);
                recip.setText(xName);
            }else{
                nr_celular.setText(x.getString(10));
                recip.setText(x.getString(4));
            }
            nr_fijo.setText(x.getString(9));
            adre_use.setText(x.getString(14));
            adre_ref.setText(x.getString(15));
            if (sc.isGPSgot) nr_direccion.setText(xAdress); else nr_direccion.setText(x.getString(14));
        }else {
            nr_fijo.setText("");
            adre_use.setText(xAdress);
            if (isLogin) recip.setText(xName); else recip.setText("");
            if(TextUtils.isEmpty(xPhone)) nr_celular.setText("");
            else nr_celular.setText(xPhone);
            if (sc.isGPSgot) nr_direccion.setText(xAdress); else nr_direccion.setText("");
        }
        x.close();
    }

    public void calculateTransporte(double tpedido) {
        double total;
        double as= distance(sc.xLatitud, sc.xLongitud,-17.387806, -66.156655);
        total = Double.parseDouble(variantTransport(as,tpedido))+tpedido;
        proLl.setVisibility(View.GONE); // progress bar y calculando
        transporte.setVisibility(View.VISIBLE);
        transporte.setText(variantTransport(as,tpedido));
        transporte_text.setVisibility(View.VISIBLE);
        total_pedido.setVisibility(View.VISIBLE);
        total_text.setVisibility(View.VISIBLE);
        splash.setVisibility(View.VISIBLE);
        total_pedido.setText(String.valueOf(total));
        isTransporte=true;
        /*
        if (sc.isGPSgot){
            Cursor loc = myDB.searchLastLocation();
            if (loc.getCount()>0){
                loc.moveToLast();
                double latitude = Double.parseDouble(loc.getString(0));
                double longitude = Double.parseDouble(loc.getString(1));
                double as= distance(latitude,longitude,-17.387806, -66.156655);
                total = Double.parseDouble(variantTransport(as,tpedido))+tpedido;
                proLl.setVisibility(View.GONE); // progress bar y calculando
                transporte.setVisibility(View.VISIBLE);
                transporte.setText(variantTransport(as,tpedido));
                transporte_text.setVisibility(View.VISIBLE);
                total_pedido.setVisibility(View.VISIBLE);
                total_text.setVisibility(View.VISIBLE);
                splash.setVisibility(View.VISIBLE);
                total_pedido.setText(String.valueOf(total));
            }else{
                String precio = "0";
                transporte.setText(precio);
            }
            total_pedido.setText(String.valueOf(total));
        }else{
            String precio = "10";
            transporte.setText(precio);
            total_pedido.setText(String.valueOf(tpedido));
        }*/
    }

    private String variantTransport(double as, double tot) {
     //   Log.d(TAG," 205 calcular trasnporte "+as+" dsd "+tot);
        String res="";
        int dis = (int) Math.round(as);
        if (dis<=2) res="8.0";
        if (dis<=4&&dis>2) res="10.0";
        if (dis<=6&&dis>4) res="12.0";
        if (dis<=10&&dis>6) res="15.0";
        if (dis>10) res="20.0";
        if (tot>sc.ceroTransporte){ if (dis<=10)res="0.0";else res="10.0";transporteInfo.setVisibility(View.VISIBLE);
        }else{transporteInfo.setVisibility(View.GONE);}
        return  res;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist/0.62137);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public void eraseBadge(){sc.initBadge(mContext,"1",true);}

    public void totalDescrip(){
        if (rlNoGps.getVisibility()==View.VISIBLE){
            RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, R.id.fp_rl_nogps);
            costtotal.setLayoutParams(params);
        }
        costtotal.setVisibility(View.VISIBLE);
    }
    public void totalnotvisible(){
        costtotal.setVisibility(View.GONE);
    }

    public void closeFragment(){
        requireActivity().onBackPressed();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity)mContext).activateAll();
    }

    private void edit_recipiente() {
        if (!editado_r) {
            recip.setEnabled(true);
            edit_recip.setText(R.string.txt_guardar);
//            recip.setKeyListener(originalKeyListener);
            recip.requestFocus();
            recip.setSelection(recip.getText().length());
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(recip, InputMethodManager.SHOW_IMPLICIT);
            editado_r = true;
        } else {
            editado_r = false;
            edit_recip.setText(R.string.txt_editar);
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(recip.getWindowToken(), 0);
            // Make it non-editable again.
            recip.setKeyListener(null);
            recip.setEnabled(false);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener((Activity) mContext, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        assert user != null;
                        xPhone = user.getPhoneNumber();
                        xId = user.getUid();
                        isLogin=true;
                        dialogSMS.dismiss();
                        getTokenFirebaseMessaging(xPhone,"",xId,"si",""); // onLogin
                        dialogComplete();
                        progressDialog.dismiss();
                        //Toast.makeText(mContext, "Your Account has been created successfully! "+ phone, Toast.LENGTH_SHORT).show();
                        // Update UI
                    } else {
                        // Sign in failed, display a message and update the UI
                        //Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            //TODO manejar el error
                            hidekeyboard();
                            Snackbar.make(rlContainer, R.string.snkbar_error,
                                    Snackbar.LENGTH_SHORT)
                                    .show();
                            progressDialog.dismiss();
                        }
                    }
                });
    }

    private void hidekeyboard() {
        InputMethodManager inputManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void dialogComplete() {
        dialogDone = new Dialog(mContext, R.style.MyCustomTheme);
        dialogDone.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDone.setCancelable(false);
        dialogDone.setContentView(R.layout.dialogdone);
        dialogDone.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        EditText nom_usuario = dialogDone.findViewById(R.id.sms_com_edit_nom);
        Button btnFin = dialogDone.findViewById(R.id.sms_com_btn);
        btnFin.setOnClickListener(view -> {
            xName = nom_usuario.getText().toString();
            //saveInfoLogin(id,phone, xName,"si");
            dialogDone.dismiss();
            if (!TextUtils.equals(xName,"")) recip.setText(xName);
        });
        dialogDone.show();
    }

    private void dialognoGPS(String tel) {
        String dir = adre_use.getText().toString();
        Dialog d = new Dialog(mContext, R.style.MyCustomTheme);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(false);
        d.setContentView(R.layout.dialognogps);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        TextView nogpsAddress = d.findViewById(R.id.nogps_address);
        TextView subTot = d.findViewById(R.id.nogps_subtotal);
        TextView trans = d.findViewById(R.id.nogps_transporte);
        TextView tota = d.findViewById(R.id.nogps_total);
        ImageView clo = d.findViewById(R.id.dng_close);
        subTot.setText(String.valueOf(xtotalPedido));
        double as= distance(sc.xLatitud, sc.xLongitud, -17.387806, -66.156655);
        double total = Double.parseDouble(variantTransport(as,xtotalPedido))+xtotalPedido;
        trans.setText(variantTransport(as,xtotalPedido));
        tota.setText(String.valueOf(total));
        nogpsAddress.setText(dir);
        TextView btnmap = d.findViewById(R.id.nogps_con_mapa);
        TextView btnreus = d.findViewById(R.id.nogps_sin_mapa);
        clo.setOnClickListener(view -> {
            fpview.setVisibility(View.GONE);
            procesarPedido.setEnabled(true);
            d.dismiss();
        });
        btnmap.setOnClickListener(view -> {
            fpview.setVisibility(View.GONE);
            d.dismiss();
            MapsFragment nextFrag= new MapsFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.frag_container,nextFrag,"mapsFragment")
                    .addToBackStack("mapsFragment")
                    .commit();
            procesarPedido.setEnabled(true);
        });
        btnreus.setOnClickListener(view -> {
            d.dismiss();
            dialogPreConfirm(total,dir,tel,trans.getText().toString(),tota.getText().toString(),"nogps");
        });
        d.show();
    }

    private void dialogPreConfirm(double total,String direcc,String celular,String trans,String tota,String comefrom) {
        String dir = nr_direccion.getText().toString();
        Dialog d = new Dialog(mContext, R.style.MyCustomTheme);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(false);
        d.setContentView(R.layout.dialog_pre_confirm);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        TextView add = d.findViewById(R.id.dpc_add);
        TextView tot = d.findViewById(R.id.dpc_total);
        ImageView clo = d.findViewById(R.id.dpc_close);
        Button okB = d.findViewById(R.id.dpc_btnok);
        RadioButton rbi = d.findViewById(R.id.dpc_rbi);
        RadioButton rbp = d.findViewById(R.id.dpc_rbp);


        tot.setText(String.valueOf(total));
        add.setText(dir);
        clo.setOnClickListener(view -> {
            fpview.setVisibility(View.GONE);
            d.dismiss();
            procesarPedido.setEnabled(true);
        });
        okB.setOnClickListener(view -> {
            boolean deltime;
            deltime= rbi.isChecked(); // true if is inmediate, false if is later
            showProgress();
            if (comefrom.equals("nogps")) continuarProcesar(dir, celular,trans,tota,"nogps",deltime);
            else continuarProcesar(direcc,celular,"","","",deltime);
            fpview.setVisibility(View.GONE);
            d.dismiss();
            procesarPedido.setEnabled(true);
        });
        d.show();
    }

    private void getTokenFirebaseMessaging(String celular,String entre,String id,String ver,String total) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            xToken = task.getResult();
            saveInfoLogin(id,celular,entre,ver,total); //notlogin
        }).addOnFailureListener(e -> {
            xToken = "token";
            saveInfoLogin(celular,celular,entre,ver,total);
        } );//notlogin
    }

    private void saveInfoLogin(String id, String phone, String nom_usuario,String ver,String totalUser) {
        String phones;
        if (phone.contains("+591")) phones = phone; else phones = "+591"+phone;
        String fecha = sc.fecha("fecha");
        String phoneDB = myDB.getUserName(1);
        if (!phones.equals(phoneDB)) myDB.addUser(id,nom_usuario,phones,fecha,totalUser);
        else  totalUser = myDB.updateUser(phones,totalUser);
        User user = new User(id,nom_usuario,phones,fecha,totalUser,ver,xToken);
        db.collection("users").document(phones)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    if (isLogin){
                        btnVerificar.animate().setDuration(500).alpha(0).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                btnVerificar.setAlpha(0);
                                verify.animate().setDuration(500).alpha(1).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        phoneVerificado();
                                    }
                                });
                            }
                        });
                    }else{
                        //TODO hacer algo cuando no se pueda verificar
                        Snackbar.make(rlContainer, R.string.snkbar_error,
                                Snackbar.LENGTH_SHORT)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {});
    }

    public void updateAdMapa (){
        if (isMapa) nr_direccion.setText(xAdress);
        proLl.setVisibility(View.VISIBLE);
    }

    public void culateTransportefromMap (){
        double as= distance(sc.xLatitud, sc.xLongitud, -17.387806, -66.156655);
        transporte.setText(variantTransport(as,xtotalPedido));
        double total = Double.parseDouble(variantTransport(as,xtotalPedido))+xtotalPedido;
        rlNoGps.setVisibility(View.GONE);
        proLl.setVisibility(View.GONE);
        transporte.setVisibility(View.VISIBLE);
        transporte_text.setVisibility(View.VISIBLE);
        total_pedido.setVisibility(View.VISIBLE);
        total_text.setVisibility(View.VISIBLE);
        splash.setVisibility(View.VISIBLE);
        total_pedido.setText(String.valueOf(total));
        isTransporte=true;
    }

    public void showProgress() {
        progressDialog = new Dialog(mContext, R.style.PauseDialog);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.progress_layout);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        WindowManager.LayoutParams wmlp = progressDialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.CENTER;
        wmlp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        wmlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public void onDetach() {
        isMapa=false;
        super.onDetach();
    }
}