package com.fam4k007.videoplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fam4k007.videoplayer.VideoBrowserActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.databinding.FragmentHomeBinding
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnSelectVideo.setOnClickListener {
            startActivity(Intent(requireContext(), VideoBrowserActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.slide_in_right,
                com.fam4k007.videoplayer.R.anim.slide_out_left
            )
        }
        
        binding.btnBiliBili.setOnClickListener {
            startActivity(Intent(requireContext(), BiliBiliPlayActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.fade_in,
                com.fam4k007.videoplayer.R.anim.fade_out
            )
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
