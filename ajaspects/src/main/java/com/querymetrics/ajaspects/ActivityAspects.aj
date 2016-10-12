package com.querymetrics.ajaspects;

import android.app.Activity;
import android.os.Bundle;

/**
 * Contains all pointcuts on the {@link Activity} class.
 */
public aspect ActivityAspects
{
  /**
   * Specifies calling advice whenever a method matching the following rules gets called:
   * Class Name: android.app.Activity
   * Method Name: onCreate
   * Method Return Type: void
   * Method Parameters: Bundle
   */
  pointcut Activity_onCreate() :
    execution(* android.app.Activity.onCreate(..)) &&
    args(Bundle);

  /**
   * Advice declaration Activity_onCreate()
   */
  after () : Activity_onCreate()
  {
    //System.out.println("After Activity_onCreate()");
    Log.i("ajaspects", "After Activity_onCreate()");
  }
}
