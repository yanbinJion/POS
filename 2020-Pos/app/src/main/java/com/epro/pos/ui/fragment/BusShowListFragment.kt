package com.epro.pos.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.epro.pos.R
import com.epro.pos.listener.DataSelectedChangeEvent
import com.epro.pos.listener.RefreshShowListUIEvent
import com.epro.pos.mvp.contract.ShowListContract
import com.epro.pos.mvp.model.bean.*
import com.epro.pos.mvp.presenter.ShowListPresenter
import com.epro.pos.ui.adapter.HorizontalTabAdapter
import com.epro.pos.ui.adapter.ShowListAdapter
import com.epro.pos.ui.base.BaseShowListFragment
import com.epro.pos.ui.view.CustomSearchView
import com.epro.pos.ui.view.CustomSpinner
import com.epro.pos.ui.view.CustomTimerPicker
import com.epro.pos.utils.PosConst
import com.mike.baselib.interface_.Judgable
import com.mike.baselib.utils.*
import com.mike.baselib.view.CommonDialog
import kotlinx.android.synthetic.main.fragment_base_titlebar_show_list.*
import kotlinx.android.synthetic.main.layout_filter_goods_files_titlebar.*
import kotlinx.android.synthetic.main.layout_filter_netshop_onlineorder_titlebar.*
import kotlinx.android.synthetic.main.layout_filter_titlebar.*
import kotlinx.android.synthetic.main.layout_filter_titlebar.customTimerPicker
import kotlinx.android.synthetic.main.layout_filter_titlebar.tvClickLeft
import kotlinx.android.synthetic.main.popup_trade_senior_filter.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.padding
import com.mike.baselib.utils.toast

class BusShowListFragment<D> : BaseShowListFragment<D, ShowListContract.View<D>
        , ShowListPresenter<D>, ShowListAdapter<D>>(), ShowListContract.View<D>, View.OnClickListener, CustomTimerPicker.OnTimerConfirmClickLisener, CustomSearchView.OnQueryClickListner, CustomSpinner.OnSipnnerItemClickListner {
    override fun onDeleteGoodsFileSuccess(failList: ArrayList<String>, successList: ArrayList<String>) {
        page = 1
        getListData()
        showDeleteGoodsFilesResult(failList, successList)
    }

    val cashierItems = ArrayList<Item>()
    override fun onGetCashierListSuccsess(cashiers: ArrayList<Cashier>) {
        var item = Item(-1, getString(R.string.all_cashiers))
        cashierItems.clear()
        cashierItems.add(item)
        for (i in cashiers.indices) {
            item = Item(i, cashiers[i].name).valueContent2(cashiers[i].id)
            cashierItems.add(item)
        }
        if(showType==PosConst.GET_CASHIER_RECONCILIATION_LIST){
            customSpinner1.setData(cashierItems)
        }
    }

    override fun onLowerShelfGoodsSuccess() {
        toast(getString(R.string.removed_successfully))
        page = 1
        getListData()
    }

    override fun onSpinnerItemClick(item: Item, view: View?) {
        page = 1
        getListData()
    }

    val categoryItems = ArrayList<Item>()
    override fun onGetGoodsCategoryListSuccess(categorys: ArrayList<GoodsCategory>) {
        var item = Item(-1, getString(R.string.all_product_categories))
        categoryItems.add(item)
        for (i in categorys.indices) {
            item = Item(i, categorys[i].typeName).valueContent2(categorys[i].id)
            categoryItems.add(item)
        }
        if (showType == PosConst.GET_GOODS_FILE_LIST) {
            csCategorys.setData(categoryItems)
        } else if (showType == PosConst.GET_ORDER_SALES_RANKING_GOODS_LIST) {
            customSpinner2.setData(categoryItems)
        } else if (showType == PosConst.GET_GROSS_PROFIT_GATHER_LIST) {
            customSpinner2.setData(categoryItems)
        } else if (showType == PosConst.GET_STOCK_QUERY_LIST) {
            customSpinner2.setData(categoryItems)
        }
    }

    override fun onQueryClick(content: String) {
        page = 1
        getListData()
    }

    override fun onTimerConfirmClick() {
        if (showType == PosConst.GET_ORDER_RECORD_LIST) {
            arguments?.putString(EXTRA, "")
        }
        page = 1
        getListData()
    }

    var totalNum=0
    override fun getListDataSuccess(list: List<D>, type: String,total:Int) {
        super.getListDataSuccess(list, type,total)
        if((page>1&&total>0)||page==1){
            this.totalNum=total
        }
        updateSelectedUI()
    }

    override fun customHearder(): Int {
        if (showType == PosConst.GET_NETSHOP_ONLINE_ORDER_LIST) {
            return R.layout.layout_filter_netshop_onlineorder_titlebar
        } else if (showType == PosConst.GET_GOODS_FILE_LIST) {
            return R.layout.layout_filter_goods_files_titlebar
        } else if (showType == PosConst.GET_FINANCIAL_BILL_DETAIL) {
            return R.layout.layout_filter_financial_bill_detail
        } else {
            return R.layout.layout_filter_titlebar
        }
    }

    var seniorPop: TradeOrderSeniorFilterPopup? = null
    override fun onClick(p0: View?) {
        when (p0) {
            tvSeniorFilter -> { //高级筛选 订单记录 商品记录
                if (cashierItems.isEmpty()) {
                    toast(getString(R.string.cashier_list_is_not_loading))
                    return
                }
                if (showType == PosConst.GET_ORDER_GOODS_RECORDS) {
                    if (categoryItems.isEmpty()) {
                        toast(getString(R.string.product_category_column_is_not_loading))
                        return
                    }
                }
                if (seniorPop == null) {
                    seniorPop = TradeOrderSeniorFilterPopup.newInstance(showType)
                    seniorPop!!.categoryItems = categoryItems
                    seniorPop!!.cashierItems = cashierItems
                    seniorPop!!.onConfirmClickLisener = object : TradeOrderSeniorFilterPopup.OnConfirmClickLisener {
                        override fun onConfirmClick() {
                            page = 1
                            getListData()
                        }
                    }
                    parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, seniorPop, "trade_senior_filter").commitAllowingStateLoss()
                } else {
                    parentFragment?.childFragmentManager!!.beginTransaction().show(seniorPop).commitAllowingStateLoss()
                }
                AppUtils.closeKeyboard(activity!!)
            }
            tvClickLeft -> {
                when (showType) {
                    PosConst.GET_STOCK_GOODS_PUTIN_LIST -> { //添加商品入库
                        EditStockGoodsPutinDetailPopup.newInstance().show(childFragmentManager, "edit_stock_putin")
                    }
                    PosConst.GET_STOCK_TAKING_LIST -> { //添加库存盘点
                        EditStockTakingDetailPopup.newInstance().show(childFragmentManager, "edit_stock_taking")
                    }
                    PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST -> { //下架商品
                        val datas = getSelectedDatas(getString(R.string.please_select_a_product_first))
                        if (datas.isEmpty()) {
                            return
                        }
                        showLowerShelfGoodsDialog(datas as ArrayList<UpperShelfGoods>)
                    }
                }

            }
            llTitle.findViewById<TextView>(R.id.tvAll) -> {
                var isAllSelect = p0!!.tag as Boolean
                isAllSelect = !isAllSelect
                p0.tag = isAllSelect
                (p0 as TextView).ext_setLeftImageResource(if (isAllSelect) R.mipmap.icon_item_checked else R.mipmap.icon_item_uncheck)
                ext_setAllValue(listDataAdapter!!.mData as ArrayList<Judgable>, isAllSelect)
                listDataAdapter!!.isAllSelect = isAllSelect
                listDataAdapter!!.notifyDataSetChanged()
                updateSelectedUI()
            }
            tvDistribution -> {
                val datas = getSelectedDatas(getString(R.string.please_select_an_order_first))
                if (datas.isEmpty()) {
                    return
                }
                val orderSns = ArrayList<String>()
                for (o in datas as ArrayList<OnlineOrder>) {
                    orderSns.add(o.orderSn)
                }
                SelectShopperPopup.newInstance(orderSns).show(childFragmentManager, "select_shopper")
            }

            tvDelete -> {
                val datas = getSelectedDatas(getString(R.string.please_select_a_product))
                if (datas.isEmpty()) {
                    return
                }
                showDeleteGoodsFilesDialog(datas as ArrayList<GoodsFile>)
            }
        }
    }


    //下架商品
    fun showLowerShelfGoodsDialog(datas: ArrayList<UpperShelfGoods>) {
        CommonDialog.Builder(activity!!)
                .setTitle(getString(R.string.take_off))
                .setContent(getString(R.string.you_confirm_that_you_want_to_remove))
                .setOnConfirmListener(object : CommonDialog.OnConfirmListener {
                    override fun onClick(dialog: Dialog) {
                        dialog.dismiss()
                        mPresenter.lowerShelfGoods(datas, showType)
                    }
                })
                .create()
                .show()
    }

    //档案删除弹窗
    fun showDeleteGoodsFilesDialog(goodsFiles: ArrayList<GoodsFile>) {
        CommonDialog.Builder(activity!!)
                .setTitle(getString(R.string.delete))
                .setContent(getString(R.string.are_you_sure_you_want_to_delete_this) + goodsFiles.size + getString(R.string.articles))
                .setOnConfirmListener(object : CommonDialog.OnConfirmListener {
                    override fun onClick(dialog: Dialog) {
                        dialog.dismiss()
                        mPresenter.deleteGoodsFile(goodsFiles, PosConst.DELETE_GOODS_FILE)
                    }
                })
                .create()
                .show()
    }

    fun showDeleteGoodsFilesResult(failList: ArrayList<String>, success: ArrayList<String>) {
        CommonDialog.Builder(activity!!)
                .setContent(success.size.toString() + getString(R.string.item_files_successfully_deleted)+"\n" +
                        failList.size + getString(R.string.product_archives_already_in_business))
                .setCancelIsVisibility(false)
                .create()
                .show()

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshShowListUIEvent(event: RefreshShowListUIEvent) {
        page = 1
        getListData()
    }


    /**
     * 获取被选择的数据
     */
    private fun getSelectedDatas(alert: String): ArrayList<Judgable> {
        val jList = ArrayList<Judgable>()
        if (ext_isAllFalse(listDataAdapter!!.mData as ArrayList<Judgable>)) {
            toast(alert)
            return jList
        }
        for (j in listDataAdapter!!.mData as ArrayList<Judgable>) {
            if (j.judge()) {
                jList.add(j)
            }
        }
        return jList
    }

    /**
     * 根据选择的情况刷新界面
     */
    private fun updateSelectedUI() {
        val jList = ArrayList<Judgable>()
        if(listDataAdapter!!.mData.isNotEmpty()){
            val data=listDataAdapter!!.mData[0]
            if(data is Judgable){
                for (j in listDataAdapter!!.mData as ArrayList<Judgable>) {
                    if (j.judge()) {
                        jList.add(j)
                    }
                }
            }
        }
        if (jList.isEmpty()) {
            if (showType == PosConst.GET_NETSHOP_ONLINE_ORDER_LIST) {
                tvDistribution.alpha = 0.5F
                tvDistribution.isEnabled = false
            }else if (showType == PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST) {
                tvClickLeft.alpha = 0.5F
                tvClickLeft.isEnabled = false
            }else if (showType == PosConst.GET_GOODS_FILE_LIST){
                tvDelete.alpha = 0.5F
                tvDelete.isEnabled = false
            }
        } else {
            if (showType == PosConst.GET_NETSHOP_ONLINE_ORDER_LIST) {
                tvDistribution.alpha = 1F
                tvDistribution.isEnabled = true
            } else if (showType == PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST) {
                tvClickLeft.alpha = 1F
                tvClickLeft.isEnabled = true
            }else if (showType == PosConst.GET_GOODS_FILE_LIST){
                tvDelete.alpha = 1F
                tvDelete.isEnabled = true
            }
        }
        val event=DataSelectedChangeEvent()
        event.selectedNum=jList.size
        event.totalNum=totalNum
        event.showType=showType
        EventBus.getDefault().post(event)
    }


    companion object {
        const val SHOW_TYPE = "ShowType"
        const val EXTRA = "extra"
        fun <D> newInstance(type: String, extra: String = ""): BusShowListFragment<D> {
            val args = Bundle()
            args.putString(SHOW_TYPE, type)
            args.putString(EXTRA, extra)
            val fragment = BusShowListFragment<D>()
            fragment.setArguments(args)
            return fragment
        }

        fun <D> newInstance(): BusShowListFragment<D> {
            return newInstance("")
        }
    }

    override fun getListAdapter(list: ArrayList<D>): ShowListAdapter<D> {
        val adapter = ShowListAdapter(activity!!, list)
        adapter.textWeights = weights
        return adapter
    }

    /**
     * 高级搜索
     */
    private fun seniorSearch() {
        val startTime = customTimerPicker.getStartTimeText()
        val endTime = customTimerPicker.getEndTimeText()
        var orderSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
        var tradeType: String? = null
        var orderType: String? = null
        var payType: String? = null
        var cashierId: String? = null
        var orderStatus: String? = null
        var categoryId: String? = null
        var puserId: String? = null
        if (seniorPop != null) {
            tradeType = if (seniorPop!!.csTradeType.getCheckPosition() <= 0) null else seniorPop!!.csTradeType.getCheckPosition().toString()
            orderType = if (seniorPop!!.csOrderType.getCheckPosition() <= 0) null else seniorPop!!.csOrderType.getCheckPosition().toString()
            payType = if (seniorPop!!.csPayType.getCheckPosition() <= 0) null else seniorPop!!.csPayType.getCheckPosition().toString()
            cashierId = if (seniorPop!!.csCashier.getCheckPosition() <= 0) null else seniorPop!!.csCashier.getCheckItem()!!.content2
            orderStatus = if (seniorPop!!.csOrderStatus.getCheckPosition() <= 0) null else (seniorPop!!.csOrderStatus.getCheckPosition() + 2).toString()
            categoryId = if (seniorPop!!.csGoodsCategorys.getCheckPosition() <= 0) null else seniorPop!!.csGoodsCategorys.getCheckId().toString()
            puserId = if (TextUtils.isEmpty(seniorPop!!.etCustomer.text.toString())) null else seniorPop!!.etCustomer.text.toString()
        }
        var productSn: String? = null
        if (showType == PosConst.GET_ORDER_RECORD_LIST) {
            mPresenter.getOrderRecordList(page, startTime, endTime, orderSn, tradeType, orderType, payType, cashierId, orderStatus, puserId, productSn, showType)
        } else if (showType == PosConst.GET_ORDER_GOODS_RECORDS) {
            orderSn = null
            productSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
            mPresenter.getOrderGoodsRecordList(page, startTime, endTime, orderSn, tradeType, orderType, payType, cashierId, orderStatus, puserId, productSn, categoryId, showType)
        }
    }


    override fun getListData() {
        when (showType) {
            PosConst.GET_GOODS_FILE_LIST -> {
                val searchStr = if (TextUtils.isEmpty(csGoodsFileSearchView.getSearchText())) null else csGoodsFileSearchView.getSearchText()
                val goodTypeId = if (csCategorys.getCheckId() == null) null else csCategorys.getCheckId().toString()
                mPresenter.getGoodsFileList(page, searchStr, goodTypeId, null, showType)
                if (categoryItems.isEmpty()) {
                    mPresenter.getGoodsCategoryList("")
                }
            }
            PosConst.GET_ORDER_RECORD_LIST -> {
                val extra = arguments!!.getString(EXTRA)
                if (!TextUtils.isEmpty(extra)) {
                    arguments!!.putString(EXTRA,"")
                    var startTime = customTimerPicker.getStartTimeText()
                    var endTime = customTimerPicker.getEndTimeText()
                    val orderSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                    var tradeType: String? = null
                    val extraStrs = extra?.split("_")!!
                    if (extraStrs[0] == HomeFragment.TAG) { //首页直接跳转过来
                        endTime = DateUtils.getYesterday() + " 23:59:59"
                        when {
                            extraStrs[1].toInt() == PosConst.TRADE_TYPE_ALL -> tradeType = null
                            extraStrs[1].toInt() == PosConst.TRADE_TYPE_OUTLINE -> tradeType = extraStrs[1]
                            extraStrs[1].toInt() == PosConst.TRADE_TYPE_ONLINE -> tradeType = extraStrs[1]
                        }
                        when {
                            extraStrs[2].toInt() == PosConst.DAY -> {
                                startTime = DateUtils.getCurrentDay() + " 00:00:00"
                                endTime = DateUtils.getCurrentDay() + " 23:59:59"
                            }
                            extraStrs[2].toInt() == PosConst.WEEK -> startTime = DateUtils.getNearlySevenDay() + " 00:00:00"
                            extraStrs[2].toInt() == PosConst.MONTH -> startTime = DateUtils.getNearlyThirtyDay() + " 00:00:00"
                        }
                        customTimerPicker.setDefaultTime(startTime.split(" ")[0], endTime.split(" ")[0])
                    }
                    mPresenter.getOrderRecordList(page, startTime, endTime, orderSn, tradeType, null, null, null, null, null, null, showType)
                } else {
                    seniorSearch()
                }
                if (cashierItems.isEmpty()) {
                    mPresenter.getCashierList(PosConst.GET_CASHIER_LIST)
                }
            }
            PosConst.GET_ORDER_GOODS_RECORDS -> {
                seniorSearch()
                if (categoryItems.isEmpty()) {
                    mPresenter.getGoodsCategoryList("")
                }
                if (cashierItems.isEmpty()) {
                    mPresenter.getCashierList(PosConst.GET_CASHIER_LIST)
                }
            }
            PosConst.GET_ORDER_SALES_RANKING_GOODS_LIST -> {
                val startTime = customTimerPicker.getStartTimeText().split(" ")[0]
                val endTime = customTimerPicker.getEndTimeText().split(" ")[0]
                val productSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val tradeType = if (customSpinner1.getCheckPosition() <= 0) null else customSpinner1.getCheckPosition().toString()
                val categoryId = null
                val orderSort = if (customSpinner2.getCheckPosition() <= 0) PosConst.SORT_ORDER_DOWN else PosConst.SORT_ORDER_UP
                mPresenter.getOrderSalesRankingGoodsList(page, startTime, endTime, tradeType, productSn, categoryId, orderSort, showType)
//                if (categoryItems.isEmpty()) {
//                    mPresenter.getGoodsCategoryList("")
//                }
            }
            PosConst.GET_GROSS_PROFIT_GATHER_LIST -> {
                val startTime = customTimerPicker.getStartTimeText().split(" ")[0]
                val endTime = customTimerPicker.getEndTimeText().split(" ")[0]
                val productSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val tradeType = if (customSpinner1.getCheckPosition() <= 0) null else customSpinner1.getCheckPosition().toString()
                val categoryId = if (customSpinner2.getCheckPosition() <= 0) null else customSpinner2.getCheckId().toString()
                mPresenter.getGrossProfitGatherList(page, startTime, endTime, tradeType, productSn, categoryId, showType)
                if (categoryItems.isEmpty()) {
                    mPresenter.getGoodsCategoryList("")
                }
            }
            PosConst.GET_CASHIER_RECONCILIATION_LIST -> {
                val startTime = customTimerPicker.getStartTimeText()
                val endTime = customTimerPicker.getEndTimeText()
                val statementId = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val cashierId = if (customSpinner1.getCheckPosition() <= 0) null else customSpinner1.getCheckItem()!!.content2
                mPresenter.getCashierReconciliationList(page, startTime, endTime, statementId, cashierId, showType)
                if (cashierItems.isEmpty()) {
                    mPresenter.getCashierList(PosConst.GET_CASHIER_LIST)
                }
            }
            PosConst.GET_STOCK_QUERY_LIST -> {
                val extra = arguments!!.getString(EXTRA)
                val searchStr = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val goodTypeId = if (customSpinner2.getCheckId() == null) null else customSpinner2.getCheckId().toString()
                if (!TextUtils.isEmpty(extra)){  //首页跳转过来预警查询
                    val extraStrs = extra?.split("_")!!
                    if (extraStrs[0] == HomeFragment.TAG){
                        customSpinner1.check(1)
                        arguments!!.putString(EXTRA, "")
                    }
                }
                var isStockEaly = customSpinner1.getCheckPosition() != 0
                mPresenter.getStockQueryList(page, searchStr, goodTypeId, null, isStockEaly, showType)
                if (categoryItems.isEmpty()) {
                    mPresenter.getGoodsCategoryList("")
                }
            }
            PosConst.GET_STOCK_GOODS_PUTIN_LIST -> {
                val startTime = customTimerPicker.getStartTimeText()
                val endTime = customTimerPicker.getEndTimeText()
                val stockCode = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val stockStatus = if (customSpinner1.getCheckPosition() <= 0) null else (customSpinner1.getCheckPosition() - 1).toString()
                mPresenter.getStockGoodsPutinList(page, startTime, endTime, stockCode, stockStatus, showType)
            }
            PosConst.GET_STOCK_TAKING_LIST -> {
                val startTime = customTimerPicker.getStartTimeText()
                val endTime = customTimerPicker.getEndTimeText()
                val inventoryCode = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val inventoryStatus = if (customSpinner1.getCheckPosition() <= 0) null else (customSpinner1.getCheckPosition() - 1).toString()
                mPresenter.getStockTakingList(page, startTime, endTime, inventoryStatus, inventoryCode, showType)
            }
            PosConst.GET_STOCK_OUTIN_DETAIL -> {
                val startTime = customTimerPicker.getStartTimeText()
                val endTime = customTimerPicker.getEndTimeText()
                val searchStr = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                val stockSource = if (customSpinner1.getCheckPosition() <= 0) null else customSpinner1.getCheckPosition().toString()
                mPresenter.getStockOutinDetailList(page, startTime, endTime, searchStr, stockSource, showType)
            }
            PosConst.GET_POS_TRADE_LIST -> {
                val startTime = customTimerPicker.getStartTimeText()
                val endTime = customTimerPicker.getEndTimeText()
                val orderSn = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                mPresenter.getPosTradeList(page, startTime, endTime, orderSn, showType)
            }
            PosConst.GET_FINANCIAL_BILL_DETAIL -> {
//                val financialBill = AppBusManager.fromJson(arguments?.getString(EXTRA)!!, FinancialBill::class.java)
//                mPresenter.getFinancialBillDetail(financialBill!!.billSn, showType)
            }
            PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST -> {
                val searchStr = if (TextUtils.isEmpty(csSearchView.getSearchText())) null else csSearchView.getSearchText()
                mPresenter.getUpperShelfGoodsList(page, searchStr, showType)
            }
            PosConst.GET_NETSHOP_ONLINE_ORDER_LIST -> {
                val extra = arguments!!.getString(EXTRA)
                if (!TextUtils.isEmpty(extra)) {
                    val extraStrs = extra?.split("_")!!
                    if (extraStrs[0] == HomeFragment.TAG) { //首页直接跳转过来
                        orderStatus = extraStrs[1].toInt()
                        val data = (rvTabs.adapter as HorizontalTabAdapter).mData
                        ext_setAllFalse(data)
                        for (d in data) {
                            if (d.icon == orderStatus) {
                                d.judgeValue = true
                            }
                        }
                        (rvTabs.adapter as HorizontalTabAdapter).notifyDataSetChanged()
                        arguments!!.putString(EXTRA, "")
                    }
                }
                val mobile = if (TextUtils.isEmpty(onlineOrderSearchView.getSearchText())) null else onlineOrderSearchView.getSearchText()
                mPresenter.getOnlineOrdersList(page, orderStatus, mobile, showType)
            }
            else -> mPresenter.ShowList("")
        }
    }

    override fun initData() {
        super.initData()
        updateSelectedUI()
        listDataAdapter?.onItemClickListener = object : ShowListAdapter.OnItemClickListener<D> {
            override fun onClick(item: D) {
                when (showType) {
                    PosConst.GET_GOODS_FILE_LIST -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, GoodsFileDetailFragment.newInstance((item as GoodsFile).goodsID)).commitAllowingStateLoss()
                    }
                    PosConst.GET_ORDER_RECORD_LIST -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, OrderDetailFragment.newInstance((item as OrderRecord).orderSn, showType)).commitAllowingStateLoss()
                    }
                    PosConst.GET_ORDER_GOODS_RECORDS -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, OrderDetailFragment.newInstance((item as GoodsRecord).orderSn, showType)).commitAllowingStateLoss()
                    }
                    PosConst.GET_CASHIER_RECONCILIATION_LIST -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, CashierReconcDetailFragment.newInstance(item as CashierRecon)).commitAllowingStateLoss()
                    }
                    PosConst.GET_POS_TRADE_LIST -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, PosOrderDetailFragment.newInstance(item as PosTradeOrder)).commitAllowingStateLoss()
                    }
                    PosConst.GET_STOCK_GOODS_PUTIN_LIST -> {
                        EditStockGoodsPutinDetailPopup.newInstance(item as StockGoodsPutin).show(childFragmentManager, "edit_stock_putin")
                    }
                    PosConst.GET_STOCK_TAKING_LIST -> {
                        EditStockTakingDetailPopup.newInstance(item as StockTaking).show(childFragmentManager, "edit_stock_taking")
                    }
                    PosConst.GET_NETSHOP_ONLINE_ORDER_LIST -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, OnlineOrderDetailFragment.newInstance(item as OnlineOrder)).commitAllowingStateLoss()
                    }
                    PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST -> {
                        //parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, GoodsFileDetailFragment.newInstance((item as UpperShelfGoods).goodsID)).commitAllowingStateLoss()
                    }
                    PosConst.GET_FINANCIAL_BILL_DETAIL -> {
                        parentFragment?.childFragmentManager!!.beginTransaction().add(R.id.fragmentContent, OrderDetailFragment.newInstance((item as BillOrder).orderSn, showType)).commitAllowingStateLoss()
                    }
                }
                AppUtils.closeKeyboard(activity!!)
            }
        }

        listDataAdapter!!.onItemSelectListener = object : ShowListAdapter.OnItemSelectListener<D> {
            override fun onItemSelect(item: D) {
                val tvAllSelect = llTitle.findViewById<TextView>(R.id.tvAll)
                tvAllSelect.tag = ext_isAllTrue(listDataAdapter!!.mData as ArrayList<Judgable>)
                tvAllSelect.ext_setLeftImageResource(if (tvAllSelect.tag as Boolean) R.mipmap.icon_item_checked else R.mipmap.icon_item_uncheck)
                listDataAdapter!!.isAllSelect = tvAllSelect.tag as Boolean
                updateSelectedUI()
            }
        }
    }

    override fun getPresenter(): ShowListPresenter<D> {
        return ShowListPresenter()
    }


    var showType = ""
    var titleList = ArrayList<String>()
    var weights = ArrayList<Float>()
    override fun initView() {
        val unit = AppBusManager.getAmountUnit()
        showType = arguments?.getString(SHOW_TYPE).toString()
        super.initView()
        weights.clear()
        when (showType) {
            //TODO 商品档案
            PosConst.GET_GOODS_FILE_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.product_name), AppContext.getInstance().getString(R.string.barcode), AppContext.getInstance().getString(R.string.specification),AppContext.getInstance().getString(R.string.product_category), AppContext.getInstance().getString(R.string.product_style), AppContext.getInstance().getString(R.string.creat_purchase_price)+"($unit)", AppContext.getInstance().getString(R.string.retail_price)+"($unit)", AppContext.getInstance().getString(R.string.online_shop_price)+"($unit)")
                weights = arrayListOf(1.5F,  2F, 2F, 1F,  1.5F,  1.5F, 1.5F, 1.5F, 1.5F)
                csCategorys.onSpinnerItemClickListner = this
                csGoodsFileSearchView.onQueryClickListner = this
            }

            //TODO 订单管理
            PosConst.GET_ORDER_RECORD_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.transaction_type), AppContext.getInstance().getString(R.string.order_num), AppContext.getInstance().getString(R.string.order_generation_time), AppContext.getInstance().getString(R.string.order_type), AppContext.getInstance().getString(R.string.payment_method), AppContext.getInstance().getString(R.string.order_status), AppContext.getInstance().getString(R.string.payment_amount)+"($unit)")
                weights = arrayListOf(1F, 1F, 2F, 2F, 1F, 1F, 1F, 1F)
                tvSeniorFilter.visibility = View.VISIBLE
                tvSeniorFilter.setOnClickListener(this)
                customSpinner1.visibility = View.GONE
                csSearchView.hint = getString(R.string.please_enter_the_order_number_to_search)
                customTimerPicker.timeType = CustomTimerPicker.DAY_TYPE
            }

            PosConst.GET_ORDER_GOODS_RECORDS -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number),AppContext.getInstance().getString(R.string.transaction_type),AppContext.getInstance().getString(R.string.barcode), AppContext.getInstance().getString(R.string.order_creation_time), AppContext.getInstance().getString(R.string.product_name), AppContext.getInstance().getString(R.string.specification),AppContext.getInstance().getString(R.string.category),AppContext.getInstance().getString(R.string.cost_price)+"($unit)", AppContext.getInstance().getString(R.string.selling_price)+"($unit)", AppContext.getInstance().getString(R.string.sales_volume), AppContext.getInstance().getString(R.string.sales_amount)+"($unit)")
                weights = arrayListOf(1F, 1.5F, 2F, 2F, 2F, 1F, 1F, 1.5F, 1.5F, 1.5F, 1.5F)
                tvSeniorFilter.visibility = View.VISIBLE
                tvSeniorFilter.setOnClickListener(this)
                customSpinner1.visibility = View.GONE
                csSearchView.hint = getString(R.string.please_enter_the_product_barcode_to_search)
                customTimerPicker.timeType = CustomTimerPicker.DAY_TYPE
            }
            PosConst.GET_ORDER_SALES_RANKING_GOODS_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.transaction_type), AppContext.getInstance().getString(R.string.barcode), AppContext.getInstance().getString(R.string.product_name),AppContext.getInstance().getString(R.string.specification), AppContext.getInstance().getString(R.string.category),AppContext.getInstance().getString(R.string.sales_volume))
                weights = arrayListOf(1F, 1F, 2F, 2F, 2F, 2F, 1F)
                customSpinner2.visibility = View.VISIBLE
                customSpinner1.hint = getString(R.string.transaction_type)
                customSpinner2.hint = getString(R.string.sort_by_sales)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_trade_type).toList() as ArrayList<String>)
                customSpinner2.setStringListData(resources.getStringArray(R.array.filter_sort_order).toList() as ArrayList<String>)
                csSearchView.hint =getString(R.string.please_enter_the_product_barcode_to_search)
            }
            PosConst.GET_GROSS_PROFIT_GATHER_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.transaction_type), AppContext.getInstance().getString(R.string.barcode), AppContext.getInstance().getString(R.string.product_name),AppContext.getInstance().getString(R.string.specification), AppContext.getInstance().getString(R.string.category), AppContext.getInstance().getString(R.string.sales_volume),AppContext.getInstance().getString(R.string.gross_profit), AppContext.getInstance().getString(R.string.gross_profit_margin))
                weights = arrayListOf(1F, 1.5F, 2F, 2F, 1F, 1F, 1.5F, 1F, 1F)
                customSpinner2.visibility = View.VISIBLE
                customSpinner1.hint = getString(R.string.transaction_type)
                customSpinner2.hint = getString(R.string.product_category)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_trade_type).toList() as ArrayList<String>)
                csSearchView.hint = getString(R.string.please_enter_the_product_barcode_to_search)
            }
            PosConst.GET_CASHIER_RECONCILIATION_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.statement_number), AppContext.getInstance().getString(R.string.cashier_number), AppContext.getInstance().getString(R.string.cashier_no_point), AppContext.getInstance().getString(R.string.reconciliation_time_no_point), AppContext.getInstance().getString(R.string.total_no_point)+"($unit)")
                weights = arrayListOf(1F, 2F, 1.5F, 1F, 2F, 1F)
                csSearchView.hint = getString(R.string.please_enter_the_statement_number_to_search)
                customSpinner1.hint = getString(R.string.cashier_no_point)
            }

            //TODO 库存管理
            PosConst.GET_STOCK_QUERY_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number),AppContext.getInstance().getString(R.string.barcode),AppContext.getInstance().getString(R.string.product_name), AppContext.getInstance().getString(R.string.specification), AppContext.getInstance().getString(R.string.unit), AppContext.getInstance().getString(R.string.category), AppContext.getInstance().getString(R.string.my_job_2), AppContext.getInstance().getString(R.string.creat_purchase_price)+"($unit)",AppContext.getInstance().getString(R.string.cost_amount)+"($unit)", AppContext.getInstance().getString(R.string.retail_price)+"($unit)", AppContext.getInstance().getString(R.string.retail_amount)+"($unit)")
                weights = arrayListOf(1F, 2F, 2F, 1F, 1F, 1F, 1.5F, 1.5F, 1.5F, 1.5F, 1.5F)
                customTimerPicker.visibility = View.GONE
                customSpinner2.visibility = View.VISIBLE
                customSpinner1.hint = getString(R.string.all_products)
                customSpinner2.hint = getString(R.string.product_category)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_goods_type).toList() as ArrayList<String>)
                csSearchView.hint = getString(R.string.please_enter_the_product_barcode_product_name_to_search)
            }
            PosConst.GET_STOCK_GOODS_PUTIN_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.business_ticket_number_no_point), AppContext.getInstance().getString(R.string.document_amount)+"($unit)", AppContext.getInstance().getString(R.string.product_status_no_point), AppContext.getInstance().getString(R.string.registration_date))
                weights = arrayListOf(1F, 2F, 2F, 2F, 2F)
                tvClickLeft.visibility = View.VISIBLE
                customSpinner1.hint = getString(R.string.product_status_no_point)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_goods_stock_status).toList() as ArrayList<String>)
                csSearchView.hint = getString(R.string.please_enter_the_business_ticket_number_to_search)
            }
            PosConst.GET_STOCK_TAKING_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.inventory_number), AppContext.getInstance().getString(R.string.profit_and_loss), AppContext.getInstance().getString(R.string.inventory_status), AppContext.getInstance().getString(R.string.date))
                weights = arrayListOf(1F, 2F, 2F, 2F, 2F)
                tvClickLeft.visibility = View.VISIBLE
                customSpinner1.hint = getString(R.string.inventory_status)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_stock_check_status).toList() as ArrayList<String>)
                csSearchView.hint = getString(R.string.please_enter_a_single_number_inventory_search)
            }
            PosConst.GET_STOCK_OUTIN_DETAIL -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.date), AppContext.getInstance().getString(R.string.single_number), AppContext.getInstance().getString(R.string.sources_out_of_storage), AppContext.getInstance().getString(R.string.barcode), AppContext.getInstance().getString(R.string.product_name), AppContext.getInstance().getString(R.string.specification), AppContext.getInstance().getString(R.string.in_and_out_number), AppContext.getInstance().getString(R.string.creat_purchase_price)+"($unit)", AppContext.getInstance().getString(R.string.document_amount)+"($unit)")
                weights = arrayListOf(1F, 2F, 2F, 2F, 2F, 2F, 1F, 1.5F, 1.5F, 1.5F)
                customSpinner1.hint = getString(R.string.sources_out_of_storage)
                customSpinner1.setStringListData(resources.getStringArray(R.array.filter_stock_outin_source).toList() as ArrayList<String>)
                csSearchView.hint = getString(R.string.please_enter_the_ticket_number)
            }

            //TODO 收银交易查询
            PosConst.GET_POS_TRADE_LIST -> {
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.order_num), AppContext.getInstance().getString(R.string.order_type),AppContext.getInstance().getString(R.string.payment_method),AppContext.getInstance().getString(R.string.order_amount)+"($unit)",AppContext.getInstance().getString(R.string.order_creation_time))
                weights = arrayListOf(1F, 2F, 2F, 2F, 2F, 2F)
                customSpinner1.visibility = View.GONE
                csSearchView.hint = getString(R.string.please_enter_the_order_number_to_search)
                customTimerPicker.timeType = CustomTimerPicker.DAY_TYPE
            }

            //TODO 网店管理
            PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST -> {
                customSpinner1.visibility = View.GONE
                csSearchView.hint = getString(R.string.please_enter_the_product_name_to_search)
                customTimerPicker.visibility = View.GONE
                tvClickLeft.visibility = View.VISIBLE
                tvClickLeft.ext_setLeftImageResource(null)
                tvClickLeft.text = getString(R.string.take_off)
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.product_id), AppContext.getInstance().getString(R.string.product_name), AppContext.getInstance().getString(R.string.product_category), AppContext.getInstance().getString(R.string.online_shop_title), AppContext.getInstance().getString(R.string.online_shop_price)+"($unit)")
                weights = arrayListOf(1F, 2F, 2F, 1F, 2F, 1F)
            }

            PosConst.GET_NETSHOP_ONLINE_ORDER_LIST -> {
                val tabs = arrayListOf(AppContext.getInstance().getString(R.string.to_be_delivered), AppContext.getInstance().getString(R.string.in_delivery), AppContext.getInstance().getString(R.string.to_be_mentioned), AppContext.getInstance().getString(R.string.successful_transaction), AppContext.getInstance().getString(R.string.transaction_failed))
                val statuses = arrayListOf(PosConst.ONLINE_ORDER_STATUS_WAIT_DISTRIBUTION,
                        PosConst.ONLINE_ORDER_STATUS_DISTRIBUTION,
                        PosConst.ONLINE_ORDER_STATUS_WAIT_SELFTAKE,
                        PosConst.ONLINE_ORDER_STATUS_SUCCESS,
                        PosConst.ONLINE_ORDER_STATUS_CANCEL)
                initHorizontalTab(tabs, statuses)
                vCover.setOnClickListener(View.OnClickListener {
                    toast("功能开发中,敬请期待")
                })
                onlineOrderSearchView.setInputType(InputType.TYPE_CLASS_NUMBER)
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.order_num), AppContext.getInstance().getString(R.string.payment_method), AppContext.getInstance().getString(R.string.payment_amount)+"($unit)", AppContext.getInstance().getString(R.string.order_creation_time))
                weights = arrayListOf(1F, 2F, 2F, 1F, 2F)
            }

            //TODO 财务账单明细
            PosConst.GET_FINANCIAL_BILL_DETAIL -> {
                val tabs = arrayListOf(AppContext.getInstance().getString(R.string.billing_source), AppContext.getInstance().getString(R.string.bill_number), AppContext.getInstance().getString(R.string.order_status), AppContext.getInstance().getString(R.string.billing_time))
                //initBillItems(tabs)
                getRefreshView().setEnableLoadMore(false)
                titleList = arrayListOf(AppContext.getInstance().getString(R.string.serial_number), AppContext.getInstance().getString(R.string.transaction_type), AppContext.getInstance().getString(R.string.order_num), AppContext.getInstance().getString(R.string.transaction_completion_time), AppContext.getInstance().getString(R.string.transaction_serial_number),AppContext.getInstance().getString(R.string.total_amount)+"($unit)", AppContext.getInstance().getString(R.string.collect_commission)+"($unit)", AppContext.getInstance().getString(R.string.this_period_should_end)+"($unit)")
                weights = arrayListOf(1F, 1F, 2F, 2F, 2F, 1F, 1F, 1F)
                customTimerPicker.timeType = CustomTimerPicker.DAY_TYPE
            }
        }

        //添加头标题
        for (i in titleList.indices) {
            val tv = TextView(activity)
            if (i == 0) {
                if (showType == PosConst.GET_NETSHOP_UPPER_SHELF_GOODS_LIST || showType == PosConst.GET_GOODS_FILE_LIST || showType == PosConst.GET_NETSHOP_ONLINE_ORDER_LIST) {
                    tv.compoundDrawablePadding = DisplayManager.dip2px(16F)!!
                    tv.ext_setLeftImageResource(R.mipmap.icon_item_uncheck)
                    tv.id = R.id.tvAll
                    tv.tag = false
                    tv.setOnClickListener(this)
                }
            }
            tv.text = titleList[i]
            tv.paint.isFakeBoldText = true
            tv.gravity = Gravity.CENTER_VERTICAL
            tv.padding = DisplayManager.dip2px(5f)!!
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.toFloat())
            tv.setTextColor(resources.getColor(R.color.mainTextColor))
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, if (weights.size == 0) 1.toFloat() else weights[i])
            tv.layoutParams = params
            llTitle.addView(tv)
        }

    }

    var orderStatus = PosConst.ONLINE_ORDER_STATUS_WAIT_DISTRIBUTION
    private fun initHorizontalTab(tabs: ArrayList<String>, statuses: ArrayList<Int>) {
        rvTabs.visibility = View.VISIBLE
        rvTabs.layoutManager = GridLayoutManager(activity, tabs.size)
        val tabItems = ArrayList<Item>()
        for (i in tabs.indices) {
            tabItems.add(Item(statuses[i], tabs[i], i == 0))
        }
        var tvAll: TextView? = null
        rvTabs.adapter = HorizontalTabAdapter(activity!!, tabItems)
        (rvTabs.adapter as HorizontalTabAdapter).onItemClickListener = object : HorizontalTabAdapter.OnItemClickListener {
            override fun onItemClick(item: Item, position: Int) {
                if (tvAll == null) {
                    tvAll = llTitle.findViewById(R.id.tvAll)
                }
                tvAll!!.isEnabled=position==0
                if (listDataAdapter!!.isAllSelect) {
                    tvAll!!.ext_setLeftImageResource(if (position == 0) R.mipmap.icon_item_checked else null)
                } else {
                    tvAll!!.ext_setLeftImageResource(if (position == 0) R.mipmap.icon_item_uncheck else null)
                }
                tvDistribution.visibility = if (position == 0) View.VISIBLE else View.GONE
                orderStatus = item.icon
                page = 1
                getListData()
            }
        }
    }

//    private fun initBillItems(items: ArrayList<String>) {
//        val financialBill = AppBusManager.fromJson(arguments?.getString(EXTRA)!!, FinancialBill::class.java)
//        PosBusManager.getPayModeText(financialBill!!.payType)
//        val info = arrayListOf(PosBusManager.getPayModeText(financialBill!!.payType), financialBill.billSn, "交易成功", financialBill.startTime + "-" + financialBill.endTime)
//        rvItems.visibility = View.VISIBLE
//        rvItems.layoutManager = GridLayoutManager(activity, 2)
//        val tabItems = ArrayList<Item>()
//        for (i in items.indices) {
//            tabItems.add(Item(i, items[i]).valueContent2(info[i]))
//        }
//        rvItems.adapter = DetailChildAdapter(activity!!, tabItems)
//    }

    override fun initListener() {
        if (showType == PosConst.GET_NETSHOP_ONLINE_ORDER_LIST) {
            tvDistribution.setOnClickListener(this)
            onlineOrderSearchView.onQueryClickListner = this
        } else if (showType == PosConst.GET_GOODS_FILE_LIST) {
            tvDelete.setOnClickListener(this)
        } else if (showType == PosConst.GET_FINANCIAL_BILL_DETAIL) {

        } else {
            tvSeniorFilter.setOnClickListener(this)
            tvClickLeft.setOnClickListener(this)
            customTimerPicker.onTimerConfirmClickLisener = this
            csSearchView.onQueryClickListner = this
            customSpinner1.onSpinnerItemClickListner = this
            customSpinner2.onSpinnerItemClickListner = this
        }
    }
}


