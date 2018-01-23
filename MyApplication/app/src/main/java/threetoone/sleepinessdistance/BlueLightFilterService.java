package threetoone.sleepinessdistance;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

public class BlueLightFilterService extends Service {

    private View mView;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    @Override
    public void onCreate()
    {
        mView = new MyLoadView( this );

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT );

        mWindowManager = (WindowManager) getSystemService( WINDOW_SERVICE );
        mWindowManager.addView( mView, mParams );

        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent )
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ((WindowManager) getSystemService( WINDOW_SERVICE )).removeView(
                mView );
        mView = null;
    }

    public class MyLoadView extends View
    {

        private Paint mPaint;

        public MyLoadView( Context context )
        {
            super( context );
            mPaint = new Paint();
            mPaint.setARGB( 200, 10, 10, 10 );
        }

        @Override
        protected void onDraw( Canvas canvas )
        {
            super.onDraw( canvas );
            canvas.drawARGB( 60, 179, 13, 17 );
        }

        @Override
        protected void onAttachedToWindow()
        {
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow()
        {
            super.onDetachedFromWindow();
        }

        @Override
        protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
        {
            super.onMeasure( widthMeasureSpec, heightMeasureSpec );
        }
    }
}
