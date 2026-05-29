package com.mengty.report.repository

import com.mengty.report.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CategoryRepository : JpaRepository<Category, UUID>