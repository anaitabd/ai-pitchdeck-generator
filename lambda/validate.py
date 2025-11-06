#!/usr/bin/env python3
"""
Simple validation script to test Lambda function components without AWS dependencies.
"""
import json
import sys
from pathlib import Path

# Add lambda directory to path
sys.path.insert(0, str(Path(__file__).parent))

def test_models():
    """Test Pydantic models."""
    print("Testing Pydantic models...")
    from models import LambdaEvent, SlideContent, PitchDeckMetadata, PitchDeckOutput, CallbackPayload
    
    # Test LambdaEvent validation
    event_data = {
        "jobId": "550e8400-e29b-41d4-a716-446655440000",
        "projectId": "660e8400-e29b-41d4-a716-446655440001",
        "userId": "770e8400-e29b-41d4-a716-446655440002",
        "s3Key": "uploads/test.pdf",
        "callbackUrl": "http://localhost:8080/api/v1/generate/callback",
        "llmModel": "claude-sonnet-4-20250514"
    }
    
    event = LambdaEvent(**event_data)
    print(f"✓ LambdaEvent validated: {event.jobId}")
    
    # Test SlideContent
    slide = SlideContent(title="Test", content="Content", type="title")
    print(f"✓ SlideContent created: {slide.title}")
    
    # Test PitchDeckOutput
    metadata = PitchDeckMetadata(model="claude-sonnet-4-20250514", durationMs=1000)
    output = PitchDeckOutput(slides=[slide], metadata=metadata)
    print(f"✓ PitchDeckOutput created with {len(output.slides)} slides")
    
    # Test CallbackPayload
    callback = CallbackPayload(
        jobId=event.jobId,
        status="COMPLETED",
        outputS3Key="pitchdecks/test/result.json"
    )
    print(f"✓ CallbackPayload created: {callback.status}")
    
    # Test JSON serialization
    json_output = output.model_dump(mode='json')
    print(f"✓ JSON serialization works: {len(json.dumps(json_output))} bytes")
    
    print("\n✅ All model tests passed!\n")


def test_test_event():
    """Test the example test event."""
    print("Testing example test event...")
    from models import LambdaEvent
    
    with open('test-event.json', 'r') as f:
        event_data = json.load(f)
    
    event = LambdaEvent(**event_data)
    print(f"✓ Test event validated: jobId={event.jobId}")
    print(f"✓ Callback URL: {event.callbackUrl}")
    print(f"✓ LLM Model: {event.llmModel}")
    
    print("\n✅ Test event validation passed!\n")


def test_imports():
    """Test that all imports work."""
    print("Testing imports...")
    
    try:
        from models import LambdaEvent
        print("✓ models.py imports successfully")
    except ImportError as e:
        print(f"✗ Failed to import models: {e}")
        return False
    
    # Note: These will fail without AWS SDK installed, but syntax should be valid
    try:
        import handler
        print("✓ handler.py syntax is valid")
    except ImportError as e:
        print(f"⚠ handler.py imports failed (expected without AWS SDK): {e}")
    
    try:
        import ai_client
        print("✓ ai_client.py syntax is valid")
    except ImportError as e:
        print(f"⚠ ai_client.py imports failed (expected without dependencies): {e}")
    
    try:
        import s3_utils
        print("✓ s3_utils.py syntax is valid")
    except ImportError as e:
        print(f"⚠ s3_utils.py imports failed (expected without boto3): {e}")
    
    try:
        import callback_client
        print("✓ callback_client.py syntax is valid")
    except ImportError as e:
        print(f"⚠ callback_client.py imports failed (expected without httpx): {e}")
    
    print("\n✅ Import tests completed!\n")
    return True


def main():
    """Run all validation tests."""
    print("=" * 60)
    print("Lambda Function Validation Script")
    print("=" * 60)
    print()
    
    try:
        test_imports()
        test_models()
        test_test_event()
        
        print("=" * 60)
        print("✅ All validation tests passed!")
        print("=" * 60)
        return 0
    
    except Exception as e:
        print(f"\n❌ Validation failed: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
