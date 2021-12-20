package io.agora.scene.breakoutroom.base;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewbinding.ViewBinding;

import io.agora.example.base.BaseFragment;

public class BaseNavFragment<T extends ViewBinding> extends BaseFragment<T> {
    @NonNull
    public NavController findNavController(){
        return NavHostFragment.findNavController(this);
    }
}
