"""Unit tests for dashboard_config.py — pure dict generation, no I/O."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from dashboard_config import (
    period_selector_card,
    summary_row,
    apexcharts_card,
    build_dashboard_config,
    PERIOD_ENTITY,
    BATTERY_ENTITY,
    FUEL_ENTITY,
    SPEED_ENTITY,
    GRAPH_SPAN_TEMPLATE,
)


def test_period_selector_has_input_select():
    card = period_selector_card()
    assert card["type"] == "entities"
    entities = card["entities"]
    main = entities[0]
    assert main["entity"] == PERIOD_ENTITY


def test_period_selector_has_datetime_conditionals():
    card = period_selector_card()
    conditionals = [e for e in card["entities"] if e.get("type") == "conditional"]
    assert len(conditionals) == 2
    # Both conditionals only show when Personalizado
    for c in conditionals:
        assert c["conditions"][0]["state"] == "Personalizado"


def test_summary_row_has_three_columns():
    row = summary_row()
    assert row["type"] == "horizontal-stack"
    assert len(row["cards"]) == 3


def test_summary_row_speed_uses_mean():
    row = summary_row()
    speed_col = row["cards"][0]
    # Each column is a vertical-stack with conditional statistic cards
    preset_cards = [
        c["card"]
        for c in speed_col["cards"]
        if c.get("type") == "conditional" and c["conditions"][0]["state"] != "Personalizado"
    ]
    for c in preset_cards:
        assert c["stat_type"] == "mean"


def test_summary_row_fuel_uses_change():
    row = summary_row()
    fuel_col = row["cards"][1]
    preset_cards = [
        c["card"]
        for c in fuel_col["cards"]
        if c.get("type") == "conditional" and c["conditions"][0]["state"] != "Personalizado"
    ]
    for c in preset_cards:
        assert c["stat_type"] == "change"


def test_summary_row_battery_uses_change():
    row = summary_row()
    battery_col = row["cards"][2]
    preset_cards = [
        c["card"]
        for c in battery_col["cards"]
        if c.get("type") == "conditional" and c["conditions"][0]["state"] != "Personalizado"
    ]
    for c in preset_cards:
        assert c["stat_type"] == "change"


def test_apexcharts_card_structure():
    card = apexcharts_card("Bateria", BATTERY_ENTITY, "#4CAF50", "%")
    assert card["type"] == "custom:apexcharts-card"
    assert card["series"][0]["entity"] == BATTERY_ENTITY
    assert card["series"][0]["color"] == "#4CAF50"
    assert "graph_span" in card
    assert card["graph_span"] == GRAPH_SPAN_TEMPLATE


def test_build_dashboard_config_has_one_view():
    config = build_dashboard_config()
    assert config["title"] == "Carros"
    assert len(config["views"]) == 1


def test_build_dashboard_config_has_five_cards():
    config = build_dashboard_config()
    cards = config["views"][0]["cards"]
    assert len(cards) == 5  # period_selector, summary_row, battery, fuel, speed


def test_build_dashboard_config_cards_order():
    config = build_dashboard_config()
    cards = config["views"][0]["cards"]
    assert cards[0]["type"] == "entities"          # period selector
    assert cards[1]["type"] == "horizontal-stack"  # summary row
    assert cards[2]["type"] == "custom:apexcharts-card"  # battery
    assert cards[3]["type"] == "custom:apexcharts-card"  # fuel
    assert cards[4]["type"] == "custom:apexcharts-card"  # speed
