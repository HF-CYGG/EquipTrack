package com.equiptrack.android.ui.department.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.DepartmentStructureUpdate
import kotlin.math.roundToInt
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap

data class FlatDepartmentNode(
    val department: Department,
    val level: Int,
    val isExpanded: Boolean,
    val hasChildren: Boolean
)

@Composable
fun OrganizationTree(
    departments: List<Department>,
    selectedDepartmentId: String?,
    onSelect: (String) -> Unit,
    onEdit: ((Department) -> Unit)? = null,
    onDelete: ((Department) -> Unit)? = null,
    onUpdateStructure: ((List<DepartmentStructureUpdate>) -> Unit)? = null,
    canManage: Boolean = false
) {
    // State for expanded nodes
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    val haptic = LocalHapticFeedback.current
    
    // Flatten the tree
    val flattenedNodes = remember(departments, expandedIds) {
        val result = mutableListOf<FlatDepartmentNode>()
        val childMap = departments.groupBy { it.parentId }
        
        fun traverse(parentId: String?, level: Int) {
            val children = childMap[parentId]?.sortedWith(compareBy({ it.order }, { it.name })) ?: emptyList()
            children.forEach { dept ->
                val hasChildren = childMap.containsKey(dept.id)
                // Default expanded for better UX in this version
                val isExpanded = expandedIds.contains(dept.id) || true 
                
                result.add(FlatDepartmentNode(dept, level, expandedIds.contains(dept.id), hasChildren))
                if (expandedIds.contains(dept.id)) {
                    traverse(dept.id, level + 1)
                }
            }
        }
        traverse(null, 0)
        result
    }

    // Initialize all expanded on first load
    LaunchedEffect(departments) {
        if (expandedIds.isEmpty() && departments.isNotEmpty()) {
            expandedIds = departments.map { it.id }.toSet()
        }
    }

    var draggingItem by remember { mutableStateOf<Department?>(null) }
    var draggingItemOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) } // Relative to item
    
    // We need to know the bounds of items to determine drop target
    val itemBounds = remember { mutableMapOf<String, Float>() } // ID -> Y position (center?)
    val itemHeights = remember { mutableMapOf<String, Float>() }
    
    var hoverTargetId by remember { mutableStateOf<String?>(null) }
    var hoverType by remember { mutableStateOf<DropType>(DropType.None) } // 0: None, 1: On Top (Reparent), 2: Above/Below (Reorder)

    var boxPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    // Haptic feedback when hover target changes
    LaunchedEffect(hoverTargetId, hoverType) {
        if (draggingItem != null && hoverTargetId != null) {
             haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .onGloballyPositioned { coordinates ->
                boxPositionInWindow = coordinates.positionInWindow()
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header / Root Drop Zone (to make item top-level)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (draggingItem != null) 50.dp else 0.dp)
                    .background(
                        if (hoverTargetId == "ROOT") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                    .border(
                        width = if (hoverTargetId == "ROOT") 2.dp else 0.dp,
                        color = if (hoverTargetId == "ROOT") MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .onGloballyPositioned { coordinates ->
                         if (draggingItem != null) {
                             // No-op, just layout
                         }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (draggingItem != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("拖动到此处设为顶级部门", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            flattenedNodes.forEachIndexed { index, node ->
                val dept = node.department
                val isDragging = draggingItem?.id == dept.id
                val isHoverTarget = hoverTargetId == dept.id

                DepartmentRow(
                    node = node,
                    isSelected = dept.id == selectedDepartmentId,
                    isExpanded = node.isExpanded,
                    isDragging = isDragging,
                    isHoverTarget = isHoverTarget,
                    hoverType = if (isHoverTarget) hoverType else DropType.None,
                    onToggleExpand = { 
                        expandedIds = if (expandedIds.contains(dept.id)) {
                            expandedIds - dept.id
                        } else {
                            expandedIds + dept.id
                        }
                    },
                    onSelect = { onSelect(dept.id) },
                    onEdit = onEdit,
                    onDelete = onDelete,
                    canManage = canManage,
                    onDragStart = { offset ->
                        if (canManage) {
                            draggingItem = dept
                            dragStartOffset = offset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (canManage) {
                            draggingItemOffset += dragAmount
                            
                            // Hit testing logic would go here typically, but we need global coordinates.
                            // Simplified: We rely on the row reporting its position.
                        }
                    },
                    onDragEnd = {
                        if (canManage && draggingItem != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            handleDrop(
                                draggedItem = draggingItem!!,
                                targetId = hoverTargetId,
                                type = hoverType,
                                allNodes = flattenedNodes,
                                departments = departments,
                                onUpdate = onUpdateStructure
                            )
                        }
                        draggingItem = null
                        draggingItemOffset = Offset.Zero
                        hoverTargetId = null
                        hoverType = DropType.None
                    },
                    onPositioned = { y, height ->
                        itemBounds[dept.id] = y
                        itemHeights[dept.id] = height.toFloat()
                    }
                )
            }
        }
        
        // Global Gesture Detector Overlay
        if (canManage) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // Find which item is under start offset
                                val windowY = boxPositionInWindow.y + offset.y
                                val clickedId = findItemAt(windowY, itemBounds, itemHeights)
                                if (clickedId != null) {
                                    val item = departments.find { it.id == clickedId }
                                    if (item != null) {
                                        draggingItem = item
                                        dragStartOffset = offset
                                        draggingItemOffset = offset // Start following pointer
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingItemOffset += dragAmount
                                
                                // Hit testing
                                val windowY = boxPositionInWindow.y + draggingItemOffset.y
                                
                                // Check Root Zone (Top ~50dp relative to Box)
                                if (draggingItemOffset.y < 50f) { // Approximate
                                    hoverTargetId = "ROOT"
                                    hoverType = DropType.Reparent
                                } else {
                                    val targetId = findItemAt(windowY, itemBounds, itemHeights)
                                    if (targetId != null && targetId != draggingItem?.id) {
                                        hoverTargetId = targetId
                                        
                                        // Determine type: Top/Bottom 25% -> Reorder, Middle 50% -> Reparent
                                        val itemTop = itemBounds[targetId] ?: 0f
                                        val itemH = itemHeights[targetId] ?: 0f
                                        
                                        val ratio = (windowY - itemTop) / itemH
                                        if (ratio in 0.25..0.75) {
                                            hoverType = DropType.Reparent
                                        } else if (ratio < 0.25) {
                                            hoverType = DropType.ReorderAbove
                                        } else {
                                            hoverType = DropType.ReorderBelow
                                        }
                                    } else {
                                         if (targetId == null && draggingItemOffset.y > 0) {
                                             // Maybe end of list?
                                             hoverTargetId = null
                                             hoverType = DropType.None
                                         }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingItem != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    handleDrop(
                                        draggedItem = draggingItem!!,
                                        targetId = hoverTargetId,
                                        type = hoverType,
                                        allNodes = flattenedNodes,
                                        departments = departments,
                                        onUpdate = onUpdateStructure
                                    )
                                }
                                draggingItem = null
                                draggingItemOffset = Offset.Zero
                                hoverTargetId = null
                                hoverType = DropType.None
                            },
                            onDragCancel = {
                                draggingItem = null
                                draggingItemOffset = Offset.Zero
                                hoverTargetId = null
                                hoverType = DropType.None
                            }
                        )
                    }
            )
        }
        
        // Dragging Ghost
        if (draggingItem != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(draggingItemOffset.x.roundToInt() - 50, draggingItemOffset.y.roundToInt() - 50) } // Offset to center?
                    .zIndex(100f)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.9f
                    }
                    .shadow(12.dp, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(draggingItem!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

enum class DropType {
    None, Reparent, ReorderAbove, ReorderBelow
}

@Composable
fun DepartmentRow(
    node: FlatDepartmentNode,
    isSelected: Boolean,
    isExpanded: Boolean,
    isDragging: Boolean,
    isHoverTarget: Boolean,
    hoverType: DropType,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onEdit: ((Department) -> Unit)?,
    onDelete: ((Department) -> Unit)?,
    canManage: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onPositioned: (Float, Int) -> Unit
) {
    val department = node.department
    
    // Calculate background based on hover state
    val backgroundColor = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Lighter when dragging source
        isHoverTarget && hoverType == DropType.Reparent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        else -> Color.Transparent
    }
    
    val indicatorColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // Use window coordinates for reliable hit testing against global drag
                onPositioned(coordinates.positionInWindow().y, coordinates.size.height)
            }
            .background(backgroundColor)
            // Visual indicator for Reorder using drawBehind
            .drawBehind {
                if (isHoverTarget) {
                    val strokeWidth = 4.dp.toPx()
                    val y = when (hoverType) {
                        DropType.ReorderAbove -> 0f
                        DropType.ReorderBelow -> size.height
                        else -> -1f
                    }
                    
                    if (y >= 0) {
                        drawLine(
                            color = indicatorColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Draw circle at start
                        drawCircle(
                            color = indicatorColor,
                            radius = strokeWidth * 1.5f,
                            center = Offset(strokeWidth * 2, y)
                        )
                    }
                    
                    if (hoverType == DropType.Reparent) {
                        // Draw border
                        drawRect(
                            color = indicatorColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(start = (node.level * 24).dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
        ) {
            // Expand/Collapse Icon
            if (node.hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                 modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = department.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Actions
            if (onEdit != null || onDelete != null) {
                Row {
                    if (onEdit != null) {
                        IconButton(onClick = { onEdit(department) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(department) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

fun findItemAt(y: Float, itemBounds: Map<String, Float>, itemHeights: Map<String, Float>): String? {
    // This is naive and assumes order in map matches list or we iterate all
    // Also assumes y is in same coordinate space.
    // Since itemBounds comes from positionInParent (LazyColumn content), and pointer is relative to Box (parent of LazyColumn),
    // we need to account for scroll offset if we used positionInParent.
    // However, onGloballyPositioned + LazyColumn is tricky.
    
    // Better approach:
    // The items report their window position? No, positionInParent is relative to LazyColumn content usually?
    // Actually, positionInParent inside LazyColumn item gives position relative to the Row? No.
    
    // Let's rely on a simpler heuristic:
    // We can't easily implement robust hit testing without a proper layout info.
    // But since the list is flat, we can iterate.
    
    // Hack: Use `itemBounds` but they are unreliable due to recycling.
    // Only visible items are in `itemBounds`.
    
    for ((id, top) in itemBounds) {
        val height = itemHeights[id] ?: 0f
        if (y >= top && y <= top + height) {
            return id
        }
    }
    return null
}

fun handleDrop(
    draggedItem: Department,
    targetId: String?,
    type: DropType,
    allNodes: List<FlatDepartmentNode>,
    departments: List<Department>,
    onUpdate: ((List<DepartmentStructureUpdate>) -> Unit)?
) {
    if (onUpdate == null) return
    
    val updates = mutableListOf<DepartmentStructureUpdate>()
    
    if (targetId == "ROOT" && type == DropType.Reparent) {
        // Make root
        if (draggedItem.parentId != null) {
            updates.add(DepartmentStructureUpdate(draggedItem.id, null, departments.count { it.parentId == null } + 1))
        }
    } else if (targetId != null) {
        val targetNode = allNodes.find { it.department.id == targetId } ?: return
        val targetDept = targetNode.department
        
        when (type) {
            DropType.Reparent -> {
                // Make child of target
                // Avoid circular dependency (if target is child of dragged)
                // Since it's a tree, we need to check if targetId is a descendant of draggedItem.id
                if (!isDescendant(draggedItem.id, targetId, departments)) {
                    updates.add(DepartmentStructureUpdate(draggedItem.id, targetId, 999)) // Append to end
                }
            }
            DropType.ReorderAbove -> {
                // Become sibling of target, before target
                // Parent ID same as target
                val newParentId = targetDept.parentId
                // Reorder: We need to shift orders.
                // Simplified: Just set order = target.order - 1? No, we might have collision.
                // Best: Get all siblings, re-sort locally, then send updates.
                
                val siblings = departments.filter { it.parentId == newParentId && it.id != draggedItem.id }.sortedBy { it.order }.toMutableList()
                val targetIndex = siblings.indexOfFirst { it.id == targetId }
                if (targetIndex != -1) {
                    siblings.add(targetIndex, draggedItem)
                    // Re-assign orders
                    siblings.forEachIndexed { index, dept ->
                         updates.add(DepartmentStructureUpdate(dept.id, newParentId, index))
                    }
                }
            }
            DropType.ReorderBelow -> {
                // Become sibling of target, after target
                 val newParentId = targetDept.parentId
                 val siblings = departments.filter { it.parentId == newParentId && it.id != draggedItem.id }.sortedBy { it.order }.toMutableList()
                 val targetIndex = siblings.indexOfFirst { it.id == targetId }
                 if (targetIndex != -1) {
                     siblings.add(targetIndex + 1, draggedItem)
                     siblings.forEachIndexed { index, dept ->
                         updates.add(DepartmentStructureUpdate(dept.id, newParentId, index))
                     }
                 }
            }
            else -> {}
        }
    }
    
    if (updates.isNotEmpty()) {
        onUpdate(updates)
    }
}

fun isDescendant(ancestorId: String, potentialDescendantId: String, allDepts: List<Department>): Boolean {
    var currentId: String? = potentialDescendantId
    while (currentId != null) {
        if (currentId == ancestorId) return true
        val parent = allDepts.find { it.id == currentId }
        currentId = parent?.parentId
    }
    return false
}
