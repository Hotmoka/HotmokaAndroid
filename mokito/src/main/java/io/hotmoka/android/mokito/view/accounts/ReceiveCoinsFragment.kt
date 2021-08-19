package io.hotmoka.android.mokito.view.accounts

import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.hotmoka.android.mokito.R
import io.hotmoka.android.mokito.databinding.FragmentReceiveCoinsBinding
import io.hotmoka.android.mokito.model.Account
import io.hotmoka.android.mokito.view.AbstractFragment
import io.hotmoka.beans.Coin
import io.hotmoka.views.AccountCreationHelper
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.*

class ReceiveCoinsFragment: AbstractFragment<FragmentReceiveCoinsBinding>() {
    private lateinit var receiver: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = ReceiveCoinsFragmentArgs.fromBundle(requireArguments()).receiver
    }

    override fun onStart() {
        super.onStart()
        setSubtitle(getString(R.string.pay_to, receiver.name))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setBinding(FragmentReceiveCoinsBinding.inflate(inflater, container, false))

        if (receiver.isKey())
            binding.anonymousDescription.text = getString(
                R.string.anonymous_description,
                AccountCreationHelper.EXTRA_GAS_FOR_ANONYMOUS
            )
        else {
            binding.anonymousDescription.visibility = View.GONE
            binding.anonymous.visibility = View.GONE
        }

        binding.amount.hint = getString(R.string.amount_to_receive)
        binding.heading.text = getString(R.string.receive_message)
        binding.showQr.setOnClickListener {
            closeKeyboard()
            showQrCode()
        }

        return binding.root
    }

    private fun showQrCode() {
        val coinWeight = binding.coinType.selectedItemPosition
        val withDecimals: BigDecimal

        try {
            withDecimals = BigDecimal(binding.amount.text.toString())
        }
        catch (e: NumberFormatException) {
            notifyUser(getString(R.string.illegal_amount_to_receive))
            return
        }

        val amount = Coin.level(coinWeight + 1, withDecimals)

        val receiverName = if (receiver.isKey())
            receiver.name else receiver.reference.toString()

        val message = "$receiverName&$amount&${binding.anonymous.isChecked}"

        Log.d("Receive", "data in the QR code: $message")

        binding.bitmap.setImageBitmap(createQRCode(message, Charsets.UTF_8, binding.bitmap.width, binding.bitmap.width))
    }

    @Throws(UnsupportedEncodingException::class, WriterException::class)
    private fun createQRCode(message: String, charset: Charset, width: Int, height: Int): Bitmap {
        val hintMap: MutableMap<EncodeHintType, ErrorCorrectionLevel> = EnumMap(EncodeHintType::class.java)
        hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val matrix: BitMatrix =
            MultiFormatWriter().encode(
                String(message.toByteArray(charset), charset),
                BarcodeFormat.QR_CODE,
                width,
                height,
                hintMap
            )

        val pixels = IntArray(width * height)
        var pos = 0
        for (y in 0 until height) for (x in 0 until width)
            pixels[pos++] = if (matrix.get(x, y)) BLACK else WHITE

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}