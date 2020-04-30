package com.example.nfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.nfc.tech.Ndef;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private final String DEVICE_ID = "device_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        setEventListeners();
    }

    public void setEventListeners() {
        setOnGenerateUUIDListener();
    }

    private void setOnGenerateUUIDListener() {
        MaterialButton generateUUID = findViewById(R.id.generate_uuid);
        generateUUID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UUID uuid = UUID.randomUUID();
                TextInputEditText deviceId = findViewById(R.id.write_device_id);
                deviceId.setText(uuid.toString());
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // Retrieve Tag object
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                Ndef ndef = Ndef.get(tag);
                if (ndef == null) {
                    Log.d("tag tech:", "null");
                } else {
                    SwitchMaterial writeMode = findViewById(R.id.write_mode);
                    if (writeMode.isChecked()) {
                        // write mode
                        writeNdefMessageMessage(ndef);
                    } else {
                        // read mode
                        readNdefMessage(intent);
                    }
                }
            }
        }
    }

    private void readNdefMessage(Intent intent) {
        Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (parcelables != null && parcelables.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) parcelables[0];
            NdefRecord[] ndefRecords = ndefMessage.getRecords();
            if (ndefRecords != null && ndefRecords.length > 0) {
                NdefRecord ndefRecord = ndefRecords[0];
                String message = getTextFromNdefRecord(ndefRecord);
                if (message.length() > 4) {
                    Log.d("message: ", getTextFromNdefRecord(ndefRecord));
                    // message structure TF-8{payloads}
                    // deleting 'TF-8'
                    message = message.substring(4);
                }
                String deviceId = "";
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    deviceId = jsonObject.getString(DEVICE_ID);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                TextInputEditText deviceIdView = findViewById(R.id.read_device_id);
                deviceIdView.setText(deviceId);
            } else {
                Toast.makeText(this, "No NDEF records found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No EXTRA_NDEF_MESSAGES found", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeNdefMessageMessage(Ndef ndef) {
        try {
            Log.d("tag tech:", "not null");
            if (ndef.isWritable()) {
                ndef.connect();
                JSONObject payloads = new JSONObject();
                TextInputEditText deviceIdInput = findViewById(R.id.write_device_id);
                String deviceId = "";
                if (deviceIdInput.getText() != null) {
                    deviceId = deviceIdInput.getText().toString();
                }
                payloads.put(DEVICE_ID, deviceId);
                ndef.writeNdefMessage(createNdefMessage(payloads.toString()));
                Log.d("tag tech:", "writable");
            } else {
                Log.d("tag tech:", "not writable");
            }
        } catch (IOException | FormatException | JSONException e) {
            Log.d("tag tech:", "wrong tag format");
        }
    }

    private void readNdefMessage(Ndef ndef) {

    }

    private String getTextFromNdefRecord(NdefRecord ndefRecord) {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (Exception e) {
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }
        return tagContent;
    }

    private NdefMessage createNdefMessage(String content) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("UTF-8", content);
        return new NdefMessage(new NdefRecord[]{ndefRecord});
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilters = new IntentFilter[]{};

        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        }
    }
}
