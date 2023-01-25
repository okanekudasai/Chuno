package com.leesfamily.chuno.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.leesfamily.chuno.R
import com.leesfamily.chuno.databinding.FragmentPermissionBinding

class PermissionFragment : Fragment() {
    private lateinit var binding : FragmentPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPermissionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val okButton = binding.okButton
        okButton.setOnClickListener {
            findNavController().navigate(R.id.loginFragment, null)
        }
    }

    companion object {

    }
}