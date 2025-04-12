import numpy as np
import time
from typing import List, Tuple
import sys
from collections import deque

@dataclass
class Matching:
    left_pairs: List[int] # Maps L vertices to their matched R vertices (-1 if unmatched)
    right_pairs: List[int]  # Maps R vertices to their matched L vertices (-1 if unmatched)
    total_weight: int  # Total weight of the matching

"""
Computes the maximum weight matching with runtime O(E^1.4 + LR) estimated from experimental tests 
on random graphs where |L| <= |R|. For more details, see https://arxiv.org/abs/2502.20889.

Args:
    L_size: Number of vertices in left partition (L)
    R_size: Number of vertices in right partition (R)
    adj: Adjacency list where each element is a list of (vertex, weight) tuples representing 
         edges from a vertex in L to vertices in R.

Note that integer weights are not required, whereas it could probably accelerate the algorithm.
"""
def kwok(L_size: int, R_size: int, adj: List[List[Tuple[int, int]]]) -> Matching:
    left_pairs = [-1] * L_size
    right_pairs = [-1] * R_size
    right_parents = [-1] * R_size
    right_visited = [False] * R_size

    visited_lefts = []
    visited_rights = []
    on_edge_rights = []
    right_on_edge = [False] * R_size

    left_labels = [max((w for _, w in edges), default=0) for edges in adj]
    right_labels = [0] * R_size
    slacks = [sys.maxsize] * R_size
    adjustment_times = 0
    q = deque()

    def advance(r: int) -> bool:
        right_on_edge[r] = False
        right_visited[r] = True
        visited_rights.append(r)
        l = right_pairs[r]
        if l != -1:
            q.append(l)
            visited_lefts.append(l)
            return False

        # apply the found augment path
        current_r = r
        while current_r != -1:
            l = right_parents[current_r]
            prev_r = left_pairs[l]
            left_pairs[l] = current_r
            right_pairs[current_r] = l
            current_r = prev_r
        return True
      

    def bfs_until_applies_augment_path(first_unmatched_r: int):
        while True:
            while q:
                l = q.popleft()
                if left_labels[l] == 0:
                    right_parents[first_unmatched_r] = l
                    if advance(first_unmatched_r):
                        return
                if slacks[first_unmatched_r] > left_labels[l]:
                    slacks[first_unmatched_r] = left_labels[l]
                    right_parents[first_unmatched_r] = l
                    if not right_on_edge[first_unmatched_r]:
                        on_edge_rights.append(first_unmatched_r)
                        right_on_edge[first_unmatched_r] = True

                for r, w in adj[l]:
                    if right_visited[r]:
                        continue
                    diff = left_labels[l] + right_labels[r] - w
                    if diff == 0:
                        right_parents[r] = l
                        if advance(r):
                            return
                    elif slacks[r] > diff:
                        right_parents[r] = l
                        slacks[r] = diff
                        if not right_on_edge[r]:
                            on_edge_rights.append(r)
                            right_on_edge[r] = True


            delta = sys.maxsize
            for r in on_edge_rights:
                if right_on_edge[r]:
                    delta = min(delta, slacks[r])

            for l in visited_lefts:
                left_labels[l] -= delta

            for r in visited_rights:
                right_labels[r] += delta

            for r in on_edge_rights:
                if right_on_edge[r]:
                    slacks[r] -= delta
                    if slacks[r] == 0 and advance(r):
                        return

    # initial greedy matching
    for l in range(L_size):
        for r, w in adj[l]:
            if right_pairs[r] == -1 and left_labels[l] + right_labels[r] == w:
                left_pairs[l] = r
                right_pairs[r] = l
                break

    for l in range(L_size):
        if left_pairs[l] != -1:
            continue
        q.clear()

        for r in visited_rights:
            right_visited[r] = False
        for r in on_edge_rights:
            right_on_edge[r] = False
            slacks[r] = sys.maxsize

        visited_lefts.clear()
        visited_rights.clear()
        on_edge_rights.clear()

        visited_lefts.append(l)
        q.append(l)
        first_unmatched_r = next(r for r in range(R_size) if right_pairs[r] == -1)
        bfs_until_applies_augment_path(first_unmatched_r)

    total = 0
    for l in range(L_size):
        matched = False
        for r, w in adj[l]:
            if r == left_pairs[l]:
                total += w
                matched = True
                break
        # remove virtual pairs
        if not matched:
            r = left_pairs[l]
            if r != -1:
                left_pairs[l] = -1
                right_pairs[r] = -1
    
    return Matching(
        left_pairs=left_pairs,
        right_pairs=right_pairs,
        total_weight=total
    )
