package com.example.myviewgroupflowlayout.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义ViewGroup流式布局
 * Created by lenovo on 2017/5/10.
 */

public class MyViewGroupFlowLayout extends ViewGroup {

    /**
     * 用来保存每行views的列表
     */
    private List<List<View>> mViewLinesList = new ArrayList<>();

    /**
     * 用来保存行高的列表
     */
    private List<Integer> mLineHeights = new ArrayList<>();


    public MyViewGroupFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
//        return super.generateLayoutParams(attrs);
        return new MarginLayoutParams(getContext(), attrs);//目的是使用同一个属性样式
    }

    /**
     * onMeasure中要对每一个子view进行测量，再调用onLayout去对每一个子view进行摆放
     * 思想：说简单点就是在onMeasure中去通过计算把子view按行添加到mViewLinesList，mLineHeights用来记录高
     * 代码逻辑就是for循环所有子View先measureChild再计算出宽高，通过一些运算逻辑判断是在一行还是换行情况去把子view childView都添加到mViewLinesList,当然一行添加到一个List<View>中好在onLayout中用双循环取出去摆放
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int iWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int iHeightMode = MeasureSpec.getMode(heightMeasureSpec);
        int iWidthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int iHeightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int measuredWith = 0;
        int measuredHeight = 0;
        int iCurLineW = 0;
        int iCurLineH = 0;
        //如果宽高的模式都是确定的直接认为子view的setMeasuredDimension中要传的宽高就是iWidthSpecSize和iHeightSpecSize
        if (iWidthMode == MeasureSpec.EXACTLY && iHeightMode == MeasureSpec.EXACTLY) {//
            measuredWith = iWidthSpecSize;
            measuredHeight = iHeightSpecSize;
        } else {//否则其它情况下，宽高不确定具体逻辑如下：
            int iChildWidth;
            int iChildHeight;
            int childCount = getChildCount();
            List<View> viewList = new ArrayList<>();

            //遍历子view
            for (int i = 0; i < childCount; i++) {
                View childView = getChildAt(i);

                //measureChild
                measureChild(childView, widthMeasureSpec, heightMeasureSpec);

                //子view的MarginLayoutParams
                MarginLayoutParams layoutParams = (MarginLayoutParams) childView.getLayoutParams();

                //子view的宽高=自已的宽+左右间距
                iChildWidth = childView.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
                iChildHeight = childView.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin;

                //行宽+子view的宽>一行的宽度
                if (iCurLineW + iChildWidth > iWidthSpecSize) {
                    /**1记录当前行的信息*/
                    //1.记录当前行的最大宽度，高度累加
                    measuredWith = Math.max(measuredWith, iCurLineW);
                    measuredHeight += iCurLineH;
                    //2.将当前行的viewList添加至总的mViewsList，将行高添加 至总的行高List
                    mViewLinesList.add(viewList);
                    mLineHeights.add(iCurLineH);

                    /**2.记录新一行的信息*/
                    //1.重新赋值新一行的宽高
                    iCurLineW = iChildWidth;
                    iCurLineH = iChildHeight;

                    //2.新建一行的viewlist，添加新一行的view
                    viewList = new ArrayList<View>();
                    viewList.add(childView);
                } else {//否则，即一行显示

                    //记录某行内的消息
                    //1.行内宽度的叠加、高度比较
                    iCurLineW += iChildWidth;//宽度叠加+==>宽=宽+子view的宽
                    iCurLineH = Math.max(iCurLineH, iChildHeight);

                    //2.添加至当前行的viewList中
                    viewList.add(childView);
                }

                /**3.如果正好是最后一行需要换行*/
                if (i == childCount - 1) {
                    //1.记录当前行的最大宽度，高度累加
                    measuredWith = Math.max(measuredWith, iCurLineW);
                    measuredHeight += iCurLineH;

                    //2.将当前行的viewList添加至总的mViewsList，将行高添加至总的行高的List
                    mViewLinesList.add(viewList);
                    mLineHeights.add(iCurLineH);
                }

            }

        }
        //最终目的setMeasuredDimension
        setMeasuredDimension(measuredWith, measuredHeight);
    }

    /**
     *  onLayout确定每一个子view的摆放
     *  思想：所有view在onMeasure中添加在mViewLinesList中，那么在onLayout中既然是做摆放，那就for循环去遍历mViewLinesList去取出里面的每个View，双层循环子view确定l/t/r/b位置调用子view的layout方法摆放，叠加curLeft
     *  每一行摆放之后再把curLeft清零，curTop为curTop+每个view的高进行叠加
     *  注意onLayout方法系统会多次调用，所以mViewLinesList和mLineHeights记得清空
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left, top, right, bottom;
        int curTop = 0;
        int curLeft = 0;
        int lineCount = mViewLinesList.size();
        for (int i = 0; i < lineCount; i++) {
            List<View> viewList = mViewLinesList.get(i);
            int lineViewSize = viewList.size();
            for (int j = 0; j < lineViewSize; j++) {
                View childView = viewList.get(j);
                MarginLayoutParams layoutParams = (MarginLayoutParams) childView.getLayoutParams();
                left = curLeft + layoutParams.leftMargin;
                top = curTop + layoutParams.topMargin;
                right = left + childView.getMeasuredWidth();
                bottom = top + childView.getMeasuredHeight();
                childView.layout(left, top, right, bottom);
                curLeft += childView.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
            }
            curLeft = 0;
            curTop += mLineHeights.get(i);
        }
        mViewLinesList.clear();
        mLineHeights.clear();
    }

}
