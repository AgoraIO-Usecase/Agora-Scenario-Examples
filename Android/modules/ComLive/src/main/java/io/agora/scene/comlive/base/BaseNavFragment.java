package io.agora.scene.comlive.base;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewbinding.ViewBinding;

public class BaseNavFragment<T extends ViewBinding> extends io.agora.example.base.BaseFragment<T> {
    @NonNull
    public NavController findNavController(){
        return NavHostFragment.findNavController(this);
    }
}