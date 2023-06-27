package com.signkorea.cloud.sample.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.util.Base64;

import com.lumensoft.ks.KSMyData;
import com.lumensoft.ks.KSResult;
import com.lumensoft.ks.KSUtil;
import com.signkorea.cloud.KSCertificateExt;
import com.signkorea.cloud.KSCertificateManagerExt;
import com.signkorea.cloud.Bio;
import com.lumensoft.ks.KSCertificate;
import com.lumensoft.ks.KSCertificateLoader;
import com.lumensoft.ks.KSException;
import com.lumensoft.ks.KSSign;
import com.signkorea.securedata.ProtectedData;
import com.signkorea.securedata.SecureData;
import com.yettiesoft.cloud.Client;
import com.yettiesoft.cloud.IncorrectPasscodeException;
import com.yettiesoft.cloud.InvalidLicenseException;
import com.signkorea.cloud.sample.DataBindingFragment;
import com.signkorea.cloud.sample.databinding.AlertPasswordBinding;
import com.signkorea.cloud.sample.databinding.FragmentSignListBinding;
import com.signkorea.cloud.sample.enums.CertificateOperation;
import com.signkorea.cloud.sample.utils.OnceRunnable;
import com.yettiesoft.cloud.models.Certificate;
import com.yettiesoft.cloud.models.CertificateInfo;
import com.yettiesoft.cloud.models.ExportedCertificate;
import com.yettiesoft.cloud.models.PinFailCount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.val;

public class SignListFragment extends DataBindingFragment<FragmentSignListBinding> implements Bio.Callback {
    private int certificateIndex = -1;    // 선택된 인증서 index
    private CertificateOperation operation = CertificateOperation.get;

    private List<KSCertificateExt> certificates = new ArrayList<>();

    private final KSCertificateManagerExt client = new KSCertificateManagerExt();

    private Bio bio = null;

    private final OnceRunnable initClient = new OnceRunnable(() -> {
        Consumer<Exception> onError = e -> new AlertDialog.Builder(requireContext())
            .setTitle("인증서 로딩 실패")
            .setMessage(e.getMessage())
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();

        try {
            client.init(requireContext().getApplicationContext());
        } catch (InvalidLicenseException e) {
            new AlertDialog.Builder(requireContext())
                .setTitle("인증서 로딩 실패")
                .setMessage(e.getMessage())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
        }
        client.setClientDelegate((Client.Delegate)requireActivity());

        Predicate<KSCertificateExt> certificateFilter = certInfo -> true;

        if (operation == CertificateOperation.all) {
            client.getUserCertificateListAll((certificates) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.get(certificateIndex).isCloud()) {
                    getBinding().addBio.setVisibility(View.VISIBLE);
                    getBinding().deleteBio.setVisibility(View.VISIBLE);
                    getBinding().autoBio.setVisibility(View.VISIBLE);
                }

                getBinding().cmsSign.setVisibility(View.VISIBLE);
                getBinding().briefSign.setVisibility(View.VISIBLE);
                getBinding().koscomCmsSign.setVisibility(View.VISIBLE);
                getBinding().koscomBriefSign.setVisibility(View.VISIBLE);
                getBinding().cmsSignHash.setVisibility(View.VISIBLE);
                getBinding().briefSignHash.setVisibility(View.VISIBLE);
                getBinding().koscomCmsSignHash.setVisibility(View.VISIBLE);
                getBinding().cmsSignHashWithSigningTime.setVisibility(View.VISIBLE);
                getBinding().mydataSign.setVisibility(View.VISIBLE);
                getBinding().getRandom.setVisibility(View.VISIBLE);
                getBinding().selfUserVerify.setVisibility(View.VISIBLE);
            }, onError);
        } else if ( operation == CertificateOperation.cache) {
            client.getCertificateListInfoCache(false, (certificates, cachedDate) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.get(certificateIndex).isCloud()) {
                    getBinding().addBio.setVisibility(View.VISIBLE);
                    getBinding().deleteBio.setVisibility(View.VISIBLE);
                    getBinding().autoBio.setVisibility(View.VISIBLE);
                }

                getBinding().koscomCmsSign.setVisibility(View.VISIBLE);
            }, onError);
        } else {
            client.getUserCertificateListCloud((certificates) -> {
                this.certificates = certificates.stream().filter(certificateFilter).collect(Collectors.toList());

                if (this.certificates.get(certificateIndex).isCloud()) {
                    getBinding().addBio.setVisibility(View.VISIBLE);
                    getBinding().deleteBio.setVisibility(View.VISIBLE);
                    getBinding().autoBio.setVisibility(View.VISIBLE);
                }

                getBinding().cmsSign.setVisibility(View.VISIBLE);
                getBinding().briefSign.setVisibility(View.VISIBLE);
                getBinding().koscomCmsSign.setVisibility(View.VISIBLE);
                getBinding().koscomBriefSign.setVisibility(View.VISIBLE);
                getBinding().cmsSignHash.setVisibility(View.VISIBLE);
                getBinding().briefSignHash.setVisibility(View.VISIBLE);
                getBinding().koscomCmsSignHash.setVisibility(View.VISIBLE);
                getBinding().cmsSignHashWithSigningTime.setVisibility(View.VISIBLE);
                getBinding().mydataSign.setVisibility(View.VISIBLE);
                getBinding().getRandom.setVisibility(View.VISIBLE);
                getBinding().selfUserVerify.setVisibility(View.VISIBLE);
            }, onError);
        }
    });

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        String message = null;
        if (i == KSException.FAILED_CLOUD_BIO_INVALID_PIN) {
            message = charSequence.toString();
        } else {
            message = i + " : " + charSequence;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("생체 인증 실패")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
            })
            .show();
    }

    @Override
    public void onAuthenticationFailed() {
        new AlertDialog.Builder(requireContext())
            .setTitle("생체 인증 실패")
            .setMessage("onAuthenticationFailed")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
            })
            .show();
    }

    @Override
    public void onAuthenticationSucceeded(Bio.OPERATION operation, int i, ProtectedData protectedData) {
        if (operation == Bio.OPERATION.REGIST) {
            new AlertDialog.Builder(requireContext())
                .setTitle("생체 인증 등록 성공")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
        } else {
            cloudSign(operation, protectedData);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBinding().cacheSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            getBinding().cacheSwitch.setText(checked ? "cache" : "noCache");
        });

        // 선택된 인증서 id
        certificateIndex = SignListFragmentArgs.fromBundle(getArguments()).getCertificateIndex();
        operation = SignListFragmentArgs.fromBundle(getArguments()).getOperation();

        // button 클릭 이벤트 할당
        getBinding().addBio.setOnClickListener(view1 -> addBio());
        getBinding().deleteBio.setOnClickListener(view1 -> removeBio());
        getBinding().autoBio.setOnClickListener(view1 -> autoBio());

        getBinding().cmsSign.setOnClickListener(view1 -> sign(Bio.OPERATION.CMSSIGN));
        getBinding().briefSign.setOnClickListener(viwe1 -> sign(Bio.OPERATION.BRIEFSIGN));
        getBinding().koscomCmsSign.setOnClickListener(view1 -> sign(Bio.OPERATION.KOSCOMCMSSIGN));
        getBinding().koscomBriefSign.setOnClickListener(view1 -> sign(Bio.OPERATION.KOSCOMBRIEFSIGN));
        getBinding().cmsSignHash.setOnClickListener(view1 -> sign(Bio.OPERATION.CMSSIGNHASH));
        getBinding().briefSignHash.setOnClickListener(view1 -> sign(Bio.OPERATION.BRIEFSIGNHASH));
        getBinding().koscomCmsSignHash.setOnClickListener(view1 -> sign(Bio.OPERATION.KOSCOMCMSSIGNHASH));
        getBinding().cmsSignHashWithSigningTime.setOnClickListener(view1 -> sign(Bio.OPERATION.CMSSIGNHASHWITHSIGNINGTIME));
        getBinding().mydataSign.setOnClickListener(view1 -> sign(Bio.OPERATION.MYDATASIGN));
        getBinding().getRandom.setOnClickListener(view1 -> sign(Bio.OPERATION.GETRANDOM));
        getBinding().selfUserVerify.setOnClickListener(view1 -> sign(Bio.OPERATION.SELFUSERVERIFY));

        initClient.run();

        bio = new Bio(requireActivity(), this.client);
        bio.setCallback(this);
    }

    private void sign(Bio.OPERATION type) {
        if (this.certificates.get(certificateIndex).isCloud()) {
            String id = this.certificates.get(certificateIndex).getCertInfo().getId();
            if (bio.isBio(id)) {
                bio.getBioCloud(id, type);
            } else {
                acquirePassword(true, false, "", pin -> {
                    cloudSign(type, new SecureData(pin.getBytes()));
                });
            }
        } else {
            acquirePassword(false, false, "", pin -> {
                localSign(type, new SecureData(pin.getBytes()));
            });
        }
    }

    private void cloudSign (Bio.OPERATION type, ProtectedData encryptedPin){
        Vector<String> orgCode = new Vector<>();

        ProtectedData encryptedIDV = new SecureData("7910021267821".getBytes());

        BiConsumer<byte[], Integer> completionCache = (signature, isCloud) -> {
            encryptedPin.clear();
            new AlertDialog.Builder(requireContext())
                .setTitle("전자서명 성공 [" + (isCloud == 0 ? "Cache" : "Cloud") + "]")
                .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
            Log.d("yettie", Base64.encodeToString(signature, Base64.NO_WRAP));
        };

        Consumer<byte[]> completion = signature -> {
            encryptedPin.clear();
            new AlertDialog.Builder(requireContext())
                .setTitle("클라우드(no cache) 전자서명 성공")
                .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
            Log.d("yettie", Base64.encodeToString(signature, Base64.NO_WRAP));
        };

        BiConsumer<String, Vector<byte[]>> mydataCompletion = (caOrg, sign) -> {
            encryptedPin.clear();
            String signature = makeMydataResultMessage(caOrg, orgCode, sign);

            new AlertDialog.Builder(requireContext())
                .setTitle("클라우드(no cache) 전자서명 성공")
                .setMessage(signature)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();

            try {
                KSUtil.saveFile(Environment.getExternalStorageDirectory().getPath() + "/NPKI/mydata.json", signature.getBytes());
                Log.d("koscom", signature);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("yettie", signature);
        };

        Consumer<byte[]> getRandomCompletion = random -> {
            encryptedPin.clear();
            new AlertDialog.Builder(requireContext())
                .setTitle("R(no cache) 값 획득 성공")
                .setMessage(Base64.encodeToString(random, Base64.NO_WRAP))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
            Log.d("yettie", Base64.encodeToString(random, Base64.NO_WRAP));
        };

        Runnable verifyCompletion = () -> {
            encryptedPin.clear();
            encryptedIDV.clear();
            new AlertDialog.Builder(requireContext())
                .setMessage("본인확인(no cache)이 완료되었습니다.")
                .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                }))
                .show();
        };

        Consumer<Exception> onError = exception -> {
            encryptedPin.clear();
            alertException(exception, operation.getLabel(), true);
        };

        byte[] plain = "sign plain".getBytes();
        byte[] dummy = "          ".getBytes();
        String mydataPlain = "[\n" +
            "    {\n" +
            "        \"orgCode\": \"1\",\n" +
            "        \"ucpidRequestInfo\": {\n" +
            "            \"userAgreement\": \"금융분야 마이데이터 통합인증을 위한 인증서 본인 확인서비스 이용약관, 개인정보 처리, 고유식별정보 수집·이용 및 위탁에 동의합니다.\",\n" +
            "            \"userAgreeInfo\": {\n" +
            "            \"realName\": true,\n" +
            "            \"gender\": true,\n" +
            "            \"nationalInfo\": true,\n" +
            "            \"birthDate\": true,\n" +
            "            \"ci\": true\n" +
            "            },\n" +
            "            \"ispUrlInfo\": \"www.mydata.or.kr\",\n" +
            "            \"ucpidNonce\": \"WGOcYBte6JHLi-B_KfJmMg\"\n" +
            "        },\n" +
            "        \"consentInfo\": {\n" +
            "            \"consent\": \"a계좌 정보...\",\n" +
            "            \"consentNonce\": \"djVJqSSmujAS\"\n" +
            "        }\n" +
            "    },\n" +
            "    {\n" +
            "        \"orgCode\": \"2\",\n" +
            "        \"ucpidRequestInfo\": {\n" +
            "            \"userAgreement\": \"금융분야 마이데이터 통합인증을 위한 인증서 본인 확인서비스 이용약관, 개인정보 처리, 고유식별정보 수집·이용 및 위탁에 동의합니다.\",\n" +
            "            \"userAgreeInfo\": {\n" +
            "            \"realName\": true,\n" +
            "            \"gender\": true,\n" +
            "            \"nationalInfo\": true,\n" +
            "            \"birthDate\": true,\n" +
            "            \"ci\": true\n" +
            "            },\n" +
            "            \"ispUrlInfo\": \"www.mydataservice.com\",\n" +
            "            \"ucpidNonce\": \"WGOcYBte6JHLi-B_KfJmMg\"\n" +
            "        },\n" +
            "        \"consentInfo\": {\n" +
            "            \"consent\": \"b카드 정보...\",\n" +
            "            \"consentNonce\": \"djVJqSSmujAS\"\n" +
            "        }\n" +
            "    }\n" +
            "]";

        KSCertificateExt cert = certificates.get(certificateIndex);

        boolean cache = getBinding().cacheSwitch.isChecked();

        switch (type) {
            case CMSSIGN:
                if (cache) {
                    try {
                        byte[] signature = client.CMSSignCache(cert.getId(), plain, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.CMSSign(cert.getId(), plain, encryptedPin, completion, onError);
                }
                break;
            case BRIEFSIGN:
                if (cache) {
                    try {
                        byte[] signature = client.briefSignCache(cert.getId(), plain, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.briefSign(cert.getId(), plain, encryptedPin, completion, onError);
                }
                break;
            case KOSCOMCMSSIGN:
                if (operation == CertificateOperation.cache) {
                    client.koscomCMSSignExt(cert.getId(), plain, encryptedPin, cache, completionCache, onError);
                } else {
                    if (cache) {
                        try {
                            byte[] signature = client.koscomCMSSignCache(cert.getId(), plain, encryptedPin);
                            new AlertDialog.Builder(requireContext())
                                .setTitle("클라우드(cache) 전자서명 성공")
                                .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                })
                                .show();
                        } catch (Exception e) {
                            alertException(e, operation.getLabel(), true);
                        } finally {
                            encryptedPin.clear();
                        }
                    } else {
                        client.koscomCMSSign(cert.getId(), plain, encryptedPin, completion, onError);
                    }
                }
                break;
            case KOSCOMBRIEFSIGN:
                if (cache) {
                    try {
                        byte[] signature = client.koscomBriefSignCache(cert.getId(), plain, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.koscomBriefSign(cert.getId(), plain, encryptedPin, completion, onError);
                }
                break;
            case CMSSIGNHASH:
                if (cache) {
                    try {
                        byte[] signature = client.CMSSignHashCache(cert.getId(), plain, dummy, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.CMSSignHash(cert.getId(), plain, dummy, encryptedPin, completion, onError);
                }
                break;
            case BRIEFSIGNHASH:
                if (cache) {
                    try {
                        byte[] signature = client.briefSignHashCache(cert.getId(), plain, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.briefSignHash(cert.getId(), plain, encryptedPin, completion, onError);
                }
                break;
            case KOSCOMCMSSIGNHASH:
                if (cache) {
                    try {
                        byte[] signature = client.koscomCMSSignHashCache(cert.getId(), plain, dummy, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.koscomCMSSignHash(cert.getId(), plain, dummy, encryptedPin, completion, onError);
                }
                break;
            case CMSSIGNHASHWITHSIGNINGTIME:
                if (cache) {
                    try {
                        byte[] signature = client.CMSSignHashWithSigningTimeCache(cert.getId(), plain, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.CMSSignHashWithSigningTime(cert.getId(), plain, encryptedPin, completion, onError);
                }
                break;
            case MYDATASIGN:
                // parsing mydata plain
                Vector<byte[]> signSrc = new Vector<>();

                try {
                    JSONArray myDataJsonArr = new JSONArray(mydataPlain);
                    JSONObject ele = null;
                    UCPIDParser ucpidParser = null;

                    String consentInfo;

                    for (int i = 0; i < myDataJsonArr.length(); i++) {
                        // parse json
                        ele = (JSONObject) myDataJsonArr.get(i);
                        ucpidParser = new UCPIDParser(ele);

                        // make ucpidRequestInfo message
                        byte[] ucpidRequestInfo = KSMyData.UCPIDRequestInfo(
                            ucpidParser.realName,
                            ucpidParser.gender,
                            ucpidParser.nationalInfo,
                            ucpidParser.birthDate,
                            ucpidParser.ci,
                            Base64.decode(ucpidParser.ucpidNonce, Base64.URL_SAFE),
                            ucpidParser.userAgreement.getBytes(),
                            ucpidParser.ispUrlInfo.getBytes());

                        orgCode.add(new String(ucpidParser.orgCode));
                        signSrc.add(ucpidRequestInfo);
                        signSrc.add(ucpidParser.consentInfo.getBytes());
                    }
                } catch (Exception e) {
                    alertException(e, operation.getLabel(), true);
                }

                if (cache) {
                    try {
                        Vector<byte[]> mydataSign = client.MydataSignCache(cert.getId(), signSrc, encryptedPin, ""); // 현재 시간은 ""
                        String caOrg = new String(mydataSign.remove(0));

                        String signature = makeMydataResultMessage(caOrg, orgCode, mydataSign);

                        new AlertDialog.Builder(requireContext())
                            .setTitle("클라우드(cache) 전자서명 성공")
                            .setMessage(signature == null ? "null" : signature)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.MydataSign(cert.getId(), signSrc, encryptedPin, "", mydataCompletion, onError); // 현재 시간은 ""
                }
                break;
            case GETRANDOM:
                if (cache) {
                    try {
                        byte[] signature = client.getRandomCache(cert.getId(), encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle("R(cache) 값 획득 성공")
                            .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                    }
                } else {
                    client.getRandom(cert.getId(), encryptedPin, getRandomCompletion, onError);
                }
                break;
            case SELFUSERVERIFY:
                if (cache) {
                    try {
                        int ret = client.selfUserVerifyCache(cert.getId(), encryptedIDV, encryptedPin);
                        new AlertDialog.Builder(requireContext())
                            .setTitle(ret == KSResult.SUCC ? "본인확인(cache)이 완료되었습니다." :
                                "본인확인(cache)이 실패했습니다.[" + ret + "]")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            })
                            .show();
                    } catch (Exception e) {
                        alertException(e, operation.getLabel(), true);
                    } finally {
                        encryptedPin.clear();
                        encryptedIDV.clear();
                    }
                } else {
                    client.selfUserVerify(cert.getId(), encryptedIDV, encryptedPin, verifyCompletion, onError);
                }
                break;
            default:
                break;
        }
    }

    private void localSign (Bio.OPERATION type, ProtectedData encryptedPin) {
        byte[] plain = "sign plain".getBytes();
        byte[] dummy = "          ".getBytes();

        KSCertificate cert = this.certificates.get(certificateIndex).cert;
        byte[] signature = null;
        try {
            switch (type) {
                case CMSSIGN:
                    signature = KSSign.sign(KSSign.CMS, cert, plain, encryptedPin);
                    break;
                case BRIEFSIGN:
                    signature = KSSign.sign(KSSign.BRIEF, cert, plain, encryptedPin);
                    break;
                case KOSCOMCMSSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM, cert, plain, encryptedPin);
                    break;
                case KOSCOMBRIEFSIGN:
                    signature = KSSign.sign(KSSign.KOSCOM_BRIEF, cert, plain, encryptedPin);
                    break;
                case CMSSIGNHASH:
                    signature = KSSign.sign(KSSign.CMS_HASH, cert, plain, dummy, encryptedPin);
                    break;
                case BRIEFSIGNHASH:
                    signature = KSSign.sign(KSSign.BRIEF_HASH, cert, plain, encryptedPin);
                    break;
                case KOSCOMCMSSIGNHASH:
                    signature = KSSign.sign(KSSign.KOSCOM_HASH, cert, plain, dummy, encryptedPin);
                    break;
                case CMSSIGNHASHWITHSIGNINGTIME:
                    signature = KSSign.cmsSignHashWithSigningTime(cert, plain, encryptedPin);
                    break;
                case CMSSIGNWITHSIGNINGTIME:
                    signature = KSSign.cmsSignWithSigningTime(cert, plain, encryptedPin, "".getBytes());
                    break;
                case MYDATASIGN:
                    break;
                default:
                    break;
            }

            new AlertDialog.Builder(requireContext())
                .setTitle("로컬 전자서명 성공")
                .setMessage(Base64.encodeToString(signature, Base64.NO_WRAP))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
            Log.d("yettie", Base64.encodeToString(signature, Base64.NO_WRAP));

        } catch (KSException e) {
            new AlertDialog.Builder(requireContext())
                .setTitle("로컬 전자서명 실패")
                .setMessage(e.getMessage())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .show();
        } finally {
            encryptedPin.clear();
        }
    }

    private void addBio() {
        acquirePassword(true, false, "", pin -> {
            String id = this.certificates.get(certificateIndex).getCertInfo().getId();
            if (!bio.isBio(id)) {
                bio.addBioCloud(id, new SecureData(pin.getBytes()));
            } else {
                new AlertDialog.Builder(requireContext())
                    .setTitle("생체 인증 등록")
                    .setMessage("이미 등록된 인증서입니다.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .show();
            }
        });
    }

    private void removeBio() {
        String title = "생체 인증 삭제", message = null;
        String id = this.certificates.get(certificateIndex).getCertInfo().getId();
        if (bio.isBio(id)) {
            bio.removeBioCloud(id);
            message = "등록된 생체 인증을 삭제하였습니다.";
        } else {
            message = "등록된 생체 인증이 없습니다.";
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();
    }

    private void autoBio() {
        bio.setAutoAuth(true);
        bio.setAutoAuthInterval(86400);

        new AlertDialog.Builder(requireContext())
            .setTitle("자동 생체 인증 등록")
            .setMessage("자동 생체 인증을 등록하였습니다.")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();
    }

    private void alertError(Exception exception) {
        final String errorMessage;

        if (exception instanceof IncorrectPasscodeException) {
            val failCount = Arrays.stream(((IncorrectPasscodeException) exception).getPinFailCounts())
                .findFirst()
                .map(PinFailCount::getFailed)
                .map(c -> String.format(" [%d/5]", c))
                .orElse("");

            errorMessage = "인증서 PIN이 일치하지 않습니다." + failCount;
        } else {
            errorMessage = exception.toString();
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("전자서명 실패")
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
            .show();
    }

    private void acquirePassword(boolean pinMode, boolean confirmPassword, String initialPassword, Consumer<String> completion) {
        ObservableField<String> pwd1 = new ObservableField<String>();
        ObservableField<String> pwd2 = new ObservableField<String>();

        AlertPasswordBinding binding =
            AlertPasswordBinding.inflate(LayoutInflater.from(requireContext()));

        binding.setPassword1(pwd1);
        binding.setPassword2(pwd2);
        binding.setConfirmPassword(confirmPassword);
        binding.setPinMode(pinMode);

        AlertDialog alert = new AlertDialog.Builder(requireContext())
            .setTitle("전자서명")
            .setView(binding.getRoot())
            .setPositiveButton(android.R.string.ok, (dialog, which) -> completion.accept(pwd1.get()))
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
            .create();

        if (confirmPassword) {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0 && pwd.equals(pwd2.get());
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
            pwd2.addOnPropertyChangedCallback(onPwdChanged);
        } else {
            Observable.OnPropertyChangedCallback onPwdChanged = new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    String pwd = pwd1.get();
                    boolean ok = pwd != null && pwd.length() > 0;
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ok);
                }
            };
            pwd1.addOnPropertyChangedCallback(onPwdChanged);
        }

        alert.show();

        pwd1.set(initialPassword);
        pwd2.set(initialPassword);
    }

    private byte[] decodeURLSafeBase64 (String input) {
        String strB64 = input;
        StringBuffer sb = null;
        byte[] ret = null;

        strB64 = strB64.replaceAll("\\.", "");
        strB64 = strB64.replaceAll("~", "");

        sb = new StringBuffer(strB64);
        switch(sb.length() % 4) {
            case 2: sb.append("=="); break;
            case 3: sb.append("="); break;
        }

        ret = Base64.decode(sb.toString().getBytes(), Base64.URL_SAFE);

        return ret;
    }

    private String encodeURLSafeBase64 (byte[] input) {
        String strB64 = new String(Base64.encode(input, Base64.URL_SAFE | Base64.NO_WRAP));

        int ret = strB64.indexOf("=");
        if (ret != -1) {
            strB64 = strB64.substring(0, ret);
        }

        return strB64;
    }

    private String makeMydataResultMessage (String caOrg, Vector<String> orgCode, Vector<byte[]> signedData) {
        JSONObject jsonResult = new JSONObject(), jsonSignedData = null;
        JSONArray jsonSignedDataList = new JSONArray();

        try {
            for (int i = 0; i < orgCode.size(); i++) {
                jsonSignedData = new JSONObject();

                jsonSignedData.put("orgCode", orgCode.get(i));
                jsonSignedData.put("signedPersonInfoReq", encodeURLSafeBase64(signedData.get(i * 2)));
                jsonSignedData.put("signedConsent", encodeURLSafeBase64(signedData.get((i * 2) + 1)));

                jsonSignedDataList.put(jsonSignedData);
            }

            jsonResult.put("signedDataList", jsonSignedDataList);
            jsonResult.put("caOrg", caOrg);
        } catch (Exception e) {
            return null;
        }

        return jsonResult.toString();
    }

    private class UCPIDParser {

        public String orgCode, userAgreement, ispUrlInfo, ucpidNonce, consentInfo;
        public boolean realName, gender, nationalInfo, birthDate, ci;

        public UCPIDParser (JSONObject obj) throws Exception {
            parse(obj);
        }

        private void parse (JSONObject obj) throws Exception {
            try {
                JSONObject jsonUcpidRequestInfo = null, jsonUserAgreeInfo = null, jsonConsentInfo = null;

                jsonUcpidRequestInfo = obj.getJSONObject("ucpidRequestInfo");
                orgCode = obj.getString("orgCode");
                userAgreement = jsonUcpidRequestInfo.getString("userAgreement");

                jsonUserAgreeInfo = jsonUcpidRequestInfo.getJSONObject("userAgreeInfo");
                realName = jsonUserAgreeInfo.getBoolean("realName");
                gender = jsonUserAgreeInfo.getBoolean("gender");
                nationalInfo = jsonUserAgreeInfo.getBoolean("nationalInfo");
                birthDate = jsonUserAgreeInfo.getBoolean("birthDate");
                ci = jsonUserAgreeInfo.getBoolean("ci");

                ispUrlInfo = jsonUcpidRequestInfo.getString("ispUrlInfo");
                ucpidNonce = jsonUcpidRequestInfo.getString("ucpidNonce");

                jsonConsentInfo = obj.getJSONObject("consentInfo");
                consentInfo = jsonConsentInfo.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
                throw new KSException(KSException.FAILED_CLOUD_INVALID_CLOUD_CERT);
            }
        }
    }
}
