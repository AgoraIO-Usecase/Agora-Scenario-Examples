package io.agora.sample.rtegame.base;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewbinding.ViewBinding;

public class BaseFragment<T extends ViewBinding> extends io.agora.example.base.BaseFragment<T> {
    public NavController findNavController(){
        return NavHostFragment.findNavController(this);
    }
}