package com.example.hwan.chatting;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

///암호화 import


public class MainActivity extends AppCompatActivity  {

    final int userKey = 2;
    EditText sendChatText;
    Button sendButton;
    TextView receiveMessage;
    TextView myMessage;
    private Socket socket;
//////////////////////암호화///////////////////////////////////////

    public static byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    public static String secretKey = "123456789789456123456789";
        //AES256 암호화
    public static String AES_Encode(String str)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,	IllegalBlockSizeException, BadPaddingException {

        byte[] textBytes = str.getBytes("UTF-8");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES");
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);

        return Base64.encodeToString(cipher.doFinal(textBytes), 0);
    }

    //AES256 복호화
    public static String AES_Decode(String str)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        byte[] textBytes =Base64.decode(str,0);
        //byte[] textBytes = str.getBytes("UTF-8");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        return new String(cipher.doFinal(textBytes), "UTF-8");
    }

////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendChatText = (EditText)findViewById(R.id.chat_content);
        sendButton = (Button)findViewById(R.id.send_btn);
        receiveMessage = (TextView)findViewById(R.id.chat_received);
        myMessage = (TextView)findViewById(R.id.mychat_received);

        try {
            socket = IO.socket("http://172.30.1.23:9000"); //로컬호스트 ip주소 수정하기
        }catch (Exception e) {
            Log.i("THREADSERVICE", "Server not connected");
            e.printStackTrace();
        }

        socket.connect();

        sendButton.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view)   {

                JSONObject obj = new JSONObject();
                String message = sendChatText.getText().toString(); //전송할 메시지

                try {
                    System.out.println("전송할 메세지: "+message);
                    String encode_message= AES_Encode(message);//encodemessage = 암호화된 메세지
                    System.out.println("AES256으로 암호화된 전송메세지: "+encode_message);

                    obj.put("encode_message", encode_message);//변경 message->encodemessage
                    obj.put("key", userKey);
                    socket.emit("encode_message", obj);//상동

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                    catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }) ;



        Emitter.Listener onMessageReceived = new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject received = (JSONObject)args[0];
                        String msg = null;
                        String key = null;
                        try {

                            msg = received.get("encode_message").toString(); //받는 메시지   변수 변경
                            System.out.println("수신된 암호화 메세지: "+msg);
                            msg = AES_Decode(msg);//변경
                            System.out.println("복호화된 수신메세지: "+msg);
                            key = received.get("key").toString(); //유저 식별키

                        } catch (JSONException e) {
                            e.printStackTrace();}
                        catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        } catch (InvalidAlgorithmParameterException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        if(key.equals("2")){
                            myMessage.append(msg+"\n");
                        }
                        else
                            receiveMessage.append(msg+"\n");
                    }
                });
            }
        };
        socket.on("receiveMsg", onMessageReceived);

    }
}
