package com.mengty.report.controller



import org.springframework.web.bind.annotation.*
import com.mengty.report.dto.ItemRequest
import com.mengty.report.dto.ItemWithPriceDTO
import com.mengty.report.model.Category
import com.mengty.report.model.Item
import com.mengty.report.service.MasterService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/master")
@CrossOrigin("*")
class MasterController(
    private val masterService: MasterService
) {

    @GetMapping("/categories")
    fun getCategories(): List<Category> {
        return masterService.getAllCategories()
    }

    @GetMapping("/items")
    fun getItems(): List<Item> {
        return masterService.getAllItems()
    }



    @GetMapping("/items-with-price")
    fun getItemsWithPrice(): List<ItemWithPriceDTO> {
        return masterService.getItemsWithPrice()
    }

    @PostMapping("/items")
    fun createItem(@RequestBody request: ItemRequest): Item {
        return masterService.createItem(request)
    }

    @GetMapping("/items/{itemCode}")
    fun getItemByCode(@PathVariable itemCode: String): Item {
        return masterService.getItemByCode(itemCode)
    }



    @PutMapping("/items/{itemCode}")
    fun updateItem(
        @PathVariable itemCode: String,
        @RequestBody request: ItemRequest
    ): Item {
        return masterService.updateItem(itemCode, request)
    }

    @DeleteMapping("/items/{itemCode}")
    fun deleteItem(@PathVariable itemCode: String): String {
        masterService.deleteItem(itemCode)
        return "Item deleted successfully"
    }
}