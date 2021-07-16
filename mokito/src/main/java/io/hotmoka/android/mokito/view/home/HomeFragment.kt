package io.hotmoka.android.mokito.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.hotmoka.android.mokito.view.Mokito
import io.hotmoka.android.mokito.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.button.setOnClickListener(::getTakamakaCode)
        binding.button2.setOnClickListener(::getManifest)
        binding.button3.setOnClickListener(::getNameOfSignatureAlgorithmForRequests)
        binding.button4.setOnClickListener(::complex)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getContext(): Mokito {
        return super.getContext() as Mokito
    }

    private fun getTakamakaCode(view: View) {
        ioScope.launch {
            try {
                val takamakaCode = context.applicationContext.controller.getTakamakaCode()
                mainScope.launch {
                    binding.textView.text = takamakaCode.toString()
                }
            }
            catch (t: Throwable) {
                mainScope.launch { notifyException(t) }
            }
        }
    }

    private fun getManifest(view: View) {
        ioScope.launch {
            try {
                val manifest = context.applicationContext.controller.getManifest()
                mainScope.launch {
                    binding.textView.text = manifest.toString()
                }
            }
            catch (t: Throwable) {
                mainScope.launch { notifyException(t) }
            }
        }
    }

    private fun getNameOfSignatureAlgorithmForRequests(view: View) {
        ioScope.launch {
            try {
                val algorithm = context.applicationContext.controller.getNameOfSignatureAlgorithmForRequests()
                mainScope.launch {
                    binding.textView.text = algorithm
                }
            }
            catch (t: Throwable) {
                mainScope.launch { notifyException(t) }
            }
        }
    }

    private fun complex(view: View) {
        context.applicationContext.controller.let {
            ioScope.launch {
                try {
                    val response = it.getResponse(it.getManifest().transaction)
                    mainScope.launch {
                        binding.textView.text = response.toString()
                    }
                }
                catch (t: Throwable) {
                    mainScope.launch { notifyException(t) }
                }
            }
        }
    }

    /*
    private fun complex1(view: View) {
        context.applicationContext.controller.let { it ->
            ioScope.launch {
                try {
                    val request = it.getRequest(it.manifest.transaction)
                    mainScope.launch {
                        binding.textView.text = request.toString()
                    }
                } catch (t: Throwable) {
                    mainScope.launch { notifyException(t) }
                }
            }
        }
    }

    private fun complex2(view: View) {
        context.applicationContext.controller.let { it ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ioScope.launch(Dispatchers.IO) {
                    try {
                        val state: Stream<Update> = it.getState(it.manifest)
                        mainScope.launch {
                            binding.textView.text = state
                                .map(Update::toString) // only API >= 24
                                ?.collect(Collectors.joining())
                        }
                    } catch (t: Throwable) {
                        mainScope.launch { notifyException(t) }
                    }
                }
            }
            else
                notifyException(IllegalStateException("this function is only available on Android API >= 24"))
        }
    }
     */

    private fun notifyException(t: Throwable) {
        Toast.makeText(context, t.toString(), Toast.LENGTH_LONG).show()
    }
}